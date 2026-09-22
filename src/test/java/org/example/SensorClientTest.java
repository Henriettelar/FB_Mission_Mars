package org.example;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensorClientTest {

    @Test
    void sensorHasFixedTypeWhenCreated() {
        SensorClient client = new SensorClient(SensorType.TEMPERATURE);

        assertEquals(SensorType.TEMPERATURE, client.getSensorType());
        assertEquals("TEMP", client.getSensorType().getType());
    }

    @Test
    void generatedReadingUsesExpectedFormatAndRange() {
        for (SensorType sensorType : SensorType.values()) {
            String reading = new SensorClient(sensorType).generateReading();
            String[] parts = reading.split(":");

            assertEquals(2, parts.length);
            assertEquals(sensorType.getType(), parts[0]);
            double value = Double.parseDouble(parts[1]);
            assertTrue(new SensorClient(sensorType).isInRange(value, sensorType));
        }
    }

    @Test
    void generatedValuesStayWithinIssueIntervals() {
        for (SensorType sensorType : SensorType.values()) {
            for (int i = 0; i < 100; i++) {
                double generated = new SensorClient(sensorType).generateValue(sensorType);
                assertTrue(sensorType.getMinValue() <= generated && generated <= sensorType.getMaxValue(),
                        sensorType + " generated out-of-range value: " + generated);
            }
        }
    }

    @Test
    void resolvesSensorTypeFromNumericChoiceAndTypeText() {
        assertEquals(SensorType.TEMPERATURE, SensorClient.resolveSensorTypeChoice("1"));
        assertEquals(SensorType.OXYGEN, SensorClient.resolveSensorTypeChoice("O2"));
        assertEquals(SensorType.AIR_PRESSURE, SensorClient.resolveSensorTypeChoice("tryk"));
        assertEquals(SensorType.CO2, SensorClient.resolveSensorTypeChoice("4"));
    }

    @Test
    void rejectsUnknownSensorTypeChoice() {
        assertThrows(IllegalArgumentException.class, () -> SensorClient.resolveSensorTypeChoice("99"));
        assertThrows(IllegalArgumentException.class, () -> SensorClient.resolveSensorTypeChoice("HUMIDITY"));
    }

    @Test
    void clientConnectsAndSendsSensorDataToServer() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int port = serverSocket.getLocalPort();
            CountDownLatch received = new CountDownLatch(1);
            AtomicReference<String> messageRef = new AtomicReference<>();

            Thread serverThread = new Thread(() -> {
                try (Socket clientSocket = serverSocket.accept();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

                    String message = reader.readLine();
                    messageRef.set(message);
                    received.countDown();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read sensor data", e);
                }
            });
            serverThread.start();

            try (SensorClient client = new SensorClient(SensorType.TEMPERATURE, "localhost", port)) {
                client.connect();
                client.sendReading();
            }

            assertTrue(received.await(5, TimeUnit.SECONDS));
            assertNotNull(messageRef.get());
            assertTrue(messageRef.get().startsWith("TEMP:"));
            double value = Double.parseDouble(messageRef.get().substring("TEMP:".length()));
            assertTrue(new SensorClient(SensorType.TEMPERATURE).isInRange(value, SensorType.TEMPERATURE));
            serverThread.join(1000);
        }
    }
}
