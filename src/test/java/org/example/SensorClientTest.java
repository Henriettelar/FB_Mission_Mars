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
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensorClientTest {

    @Test
    void sensorHasFixedTypeWhenCreated() {
        SensorClient client = new SensorClient(SensorClient.SensorType.TEMPERATURE);

        assertEquals(SensorClient.SensorType.TEMPERATURE, client.getSensorType());
        assertEquals("TEMP", client.getSensorType().getCode());
    }

    @Test
    void generatedReadingUsesExpectedFormatAndRange() {
        for (SensorClient.SensorType sensorType : SensorClient.SensorType.values()) {
            String reading = new SensorClient(sensorType).generateReading();
            String[] parts = reading.split(":");

            assertEquals(2, parts.length);
            assertEquals(sensorType.getCode(), parts[0]);
            double value = Double.parseDouble(parts[1]);
            assertTrue(sensorType.isInRange(value));
        }
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

            try (SensorClient client = new SensorClient(SensorClient.SensorType.TEMPERATURE, "localhost", port)) {
                client.connect();
                client.sendReading();
            }

            assertTrue(received.await(5, TimeUnit.SECONDS));
            assertNotNull(messageRef.get());
            assertTrue(messageRef.get().startsWith("TEMP:"));
            double value = Double.parseDouble(messageRef.get().substring("TEMP:".length()));
            assertTrue(SensorClient.SensorType.TEMPERATURE.isInRange(value));
            serverThread.join(1000);
        }
    }
}
