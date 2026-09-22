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
        SensorHandler.ParsedMessage parsed = SensorHandler.parseMessage("TEMP:27.4 °C");

        assertEquals(SensorType.TEMPERATURE, parsed.sensorType());
        assertEquals(27.4, parsed.value(), 0.0001);
        assertEquals("°C", parsed.unit());
    }

    @Test
    void parsesOxygenMessage() {
        SensorHandler.ParsedMessage parsed = SensorHandler.parseMessage("O2:21.5 %");

        assertEquals(SensorType.OXYGEN, parsed.sensorType());
        assertEquals(21.5, parsed.value(), 0.0001);
        assertEquals("%", parsed.unit());
    }

    @Test
    void parsesCo2Message() {
        SensorHandler.ParsedMessage parsed = SensorHandler.parseMessage("CO2:2350 ppm");

        assertEquals(SensorType.CO2, parsed.sensorType());
        assertEquals(2350.0, parsed.value(), 0.0001);
        assertEquals("ppm", parsed.unit());
    }

    @Test
    void parsesTrykMessageAsAirPressure() {
        SensorHandler.ParsedMessage parsed = SensorHandler.parseMessage("TRYK:1000 hPa");

        assertEquals(SensorType.AIR_PRESSURE, parsed.sensorType());
        assertEquals(1000.0, parsed.value(), 0.0001);
        assertEquals("hPa", parsed.unit());
    }

    @Test
    void thresholdCheckAcceptsValuesWithinLimits() {
        assertTrue(SensorHandler.checkInThreshold(SensorType.TEMPERATURE, -15.0));
        assertTrue(SensorHandler.checkInThreshold(SensorType.TEMPERATURE, 35.0));
        assertTrue(SensorHandler.checkInThreshold(SensorType.OXYGEN, 19.0));
        assertTrue(SensorHandler.checkInThreshold(SensorType.OXYGEN, 23.0));
        assertTrue(SensorHandler.checkInThreshold(SensorType.AIR_PRESSURE, 800.0));
        assertTrue(SensorHandler.checkInThreshold(SensorType.AIR_PRESSURE, 1100.0));
        assertTrue(SensorHandler.checkInThreshold(SensorType.CO2, 2000.0));
    }

    @Test
    void thresholdCheckRejectsCriticalValues() {
        assertFalse(SensorHandler.checkInThreshold(SensorType.TEMPERATURE, -15.1));
        assertFalse(SensorHandler.checkInThreshold(SensorType.TEMPERATURE, 35.1));
        assertFalse(SensorHandler.checkInThreshold(SensorType.OXYGEN, 18.9));
        assertFalse(SensorHandler.checkInThreshold(SensorType.OXYGEN, 23.1));
        assertFalse(SensorHandler.checkInThreshold(SensorType.AIR_PRESSURE, 799.9));
        assertFalse(SensorHandler.checkInThreshold(SensorType.AIR_PRESSURE, 1100.1));
        assertFalse(SensorHandler.checkInThreshold(SensorType.CO2, 2000.1));
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
            assertEquals("[ERROR] Invalid numeric value", reader.readLine());

            writer.println("CO2:2350 ppm");
            String alarmResponse = reader.readLine();
            assertTrue(alarmResponse.startsWith("ALARM: CO2:2350 ppm outside allowed range"));

            client.close();
            handlerThread.join(2000);

            assertFalse(handlerThread.isAlive());
            assertTrue(sensorLog.getMessages().stream().anyMatch(message -> message.contains("Invalid numeric value")));
            assertTrue(sensorLog.getMessages().stream().anyMatch(message -> message.contains("CO2")));
        }
    }

    @Test
    void handlesEmptyMessageWithErrorFormat() throws Exception {
        SensorLog sensorLog = new SensorLog();

        try (ServerSocket serverSocket = new ServerSocket(0);
             Socket client = new Socket("localhost", serverSocket.getLocalPort());
             Socket serverSideClient = serverSocket.accept();
             PrintWriter writer = new PrintWriter(client.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()))) {

            Thread handlerThread = new Thread(new SensorHandler(serverSideClient, sensorLog));
            handlerThread.start();

            writer.println("");
            String response = reader.readLine();
            assertEquals("[ERROR] Empty message", response);

            client.close();
            handlerThread.join(2000);
            assertFalse(handlerThread.isAlive());
        }
    }

    @Test
    void handlesInvalidMessageFormatWithErrorFormat() throws Exception {
        SensorLog sensorLog = new SensorLog();

        try (ServerSocket serverSocket = new ServerSocket(0);
             Socket client = new Socket("localhost", serverSocket.getLocalPort());
             Socket serverSideClient = serverSocket.accept();
             PrintWriter writer = new PrintWriter(client.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()))) {

            Thread handlerThread = new Thread(new SensorHandler(serverSideClient, sensorLog));
            handlerThread.start();

            writer.println("TEMP-27.4");
            String response = reader.readLine();
            assertEquals("[ERROR] Message must match TYPE:VALUE", response);

            client.close();
            handlerThread.join(2000);
            assertFalse(handlerThread.isAlive());
        }
    }

    @Test
    void logsAlarmMessagesWithTimestamp() throws Exception {
        SensorLog sensorLog = new SensorLog();
        sensorLog.log("ALARM: TEMP:35.1 °C outside allowed range [ -15.0 - 35.0 ]");

        assertFalse(sensorLog.getMessages().isEmpty());
        assertTrue(sensorLog.getMessages().getFirst().matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} - ALARM: TEMP:35.1 °C outside allowed range \\[ -15.0 - 35.0 \\]"));
    }

    @Test
    void sendsAlarmWhenReadingIsOutsideThreshold() throws Exception {
        SensorLog sensorLog = new SensorLog();

        try (ServerSocket serverSocket = new ServerSocket(0);
             Socket client = new Socket("localhost", serverSocket.getLocalPort());
             Socket serverSideClient = serverSocket.accept();
             PrintWriter writer = new PrintWriter(client.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()))) {

            Thread handlerThread = new Thread(new SensorHandler(serverSideClient, sensorLog));
            handlerThread.start();

            writer.println("TEMP:-15.1 °C");
            String response = reader.readLine();
            assertTrue(response.startsWith("ALARM: TEMP:-15.1 °C outside allowed range"));

            client.close();
            handlerThread.join(2000);
            assertTrue(sensorLog.getMessages().stream().anyMatch(message -> message.contains("Threshold alarm")));
        }
    }
}
