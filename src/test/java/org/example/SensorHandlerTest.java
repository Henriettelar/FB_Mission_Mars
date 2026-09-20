package org.example;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensorHandlerTest {

    @Test
    void parsesTemperatureMessage() {
        SensorHandler.ParsedMessage parsed = SensorHandler.parseMessage("TEMP:27.4");

        assertEquals(SensorType.TEMPERATURE, parsed.sensorType());
        assertEquals(27.4, parsed.value(), 0.0001);
    }

    @Test
    void parsesOxygenMessage() {
        SensorHandler.ParsedMessage parsed = SensorHandler.parseMessage("O2:21.5");

        assertEquals(SensorType.OXYGEN, parsed.sensorType());
        assertEquals(21.5, parsed.value(), 0.0001);
    }

    @Test
    void parsesCo2Message() {
        SensorHandler.ParsedMessage parsed = SensorHandler.parseMessage("CO2:2350");

        assertEquals(SensorType.CO2, parsed.sensorType());
        assertEquals(2350.0, parsed.value(), 0.0001);
    }

    @Test
    void parsesTrykMessageAsAirPressure() {
        SensorHandler.ParsedMessage parsed = SensorHandler.parseMessage("TRYK:1000");

        assertEquals(SensorType.AIR_PRESSURE, parsed.sensorType());
        assertEquals(1000.0, parsed.value(), 0.0001);
    }

    @Test
    void rejectsInvalidMessageFormat() {
        assertThrows(IllegalArgumentException.class, () -> SensorHandler.parseMessage("TEMP-27.4"));
    }

    @Test
    void rejectsInvalidNumericValue() {
        assertThrows(NumberFormatException.class, () -> SensorHandler.parseMessage("TEMP:abc"));
    }

    @Test
    void rejectsMissingValue() {
        assertThrows(IllegalArgumentException.class, () -> SensorHandler.parseMessage("TEMP:"));
    }

    @Test
    void rejectsUnknownSensorType() {
        assertThrows(IllegalArgumentException.class, () -> SensorHandler.parseMessage("HUMIDITY:55"));
    }

    @Test
    void handlesInvalidMessageAndContinuesForNextReading() throws Exception {
        SensorLog sensorLog = new SensorLog();

        try (ServerSocket serverSocket = new ServerSocket(0);
             Socket client = new Socket("localhost", serverSocket.getLocalPort());
             Socket serverSideClient = serverSocket.accept();
             PrintWriter writer = new PrintWriter(client.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()))) {

            Thread handlerThread = new Thread(new SensorHandler(serverSideClient, sensorLog));
            handlerThread.start();

            writer.println("TEMP:abc");
            assertEquals("ERROR: Invalid numeric value", reader.readLine());

            writer.println("CO2:2350");
            assertEquals("ACK: CO2:2350", reader.readLine());

            client.close();
            handlerThread.join(2000);

            assertFalse(handlerThread.isAlive());
            assertTrue(sensorLog.getMessages().stream().anyMatch(message -> message.contains("Invalid numeric value")));
            assertTrue(sensorLog.getMessages().stream().anyMatch(message -> message.contains("type=CO2")));
        }
    }
}
