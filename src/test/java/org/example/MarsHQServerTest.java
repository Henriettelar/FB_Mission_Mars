package org.example;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarsHQServerTest {

    @Test
    void startsOnSelectedPortAndAcceptsOneClient() throws Exception {
        SensorLog sensorLog = new SensorLog();
        MarsHQServer server = new MarsHQServer(0, sensorLog);
        Thread serverThread = server.startAsync();

        try {
            waitFor(() -> server.isRunning() && server.getPort() > 0);

            try (Socket client = new Socket("localhost", server.getPort());
                 PrintWriter writer = new PrintWriter(client.getOutputStream(), true);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()))) {

                writer.println("TEMP:22.1 °C");

                assertEquals("ACK: TEMP:22.1 °C", reader.readLine());
            }

            assertTrue(sensorLog.getMessages().stream().anyMatch(message -> message.contains("Received DATA from")));
        } finally {
            server.stop();
            serverThread.join(2000);
        }
    }

    @Test
    void multipleClientsCanConnectAndBeHandled() throws Exception {
        SensorLog sensorLog = new SensorLog();
        MarsHQServer server = new MarsHQServer(0, sensorLog);
        Thread serverThread = server.startAsync();

        try {
            waitFor(() -> server.isRunning() && server.getPort() > 0);
            int port = server.getPort();

            try (Socket firstClient = new Socket("localhost", port);
                 Socket secondClient = new Socket("localhost", port);
                 PrintWriter firstWriter = new PrintWriter(firstClient.getOutputStream(), true);
                 PrintWriter secondWriter = new PrintWriter(secondClient.getOutputStream(), true);
                 BufferedReader firstReader = new BufferedReader(new InputStreamReader(firstClient.getInputStream()));
                 BufferedReader secondReader = new BufferedReader(new InputStreamReader(secondClient.getInputStream()))) {

                firstWriter.println("TEMP:21.0 °C");
                secondWriter.println("PRESSURE:1012 hPa");

                assertEquals("ACK: TEMP:21.0 °C", firstReader.readLine());
                assertEquals("ACK: PRESSURE:1012 hPa", secondReader.readLine());
            }

            assertTrue(sensorLog.getMessages().stream().filter(message -> message.contains("Received DATA from")).count() >= 2);
        } finally {
            server.stop();
            serverThread.join(2000);
        }
    }

    @Test
    void serverContinuesAfterClientDisconnect() throws Exception {
        SensorLog sensorLog = new SensorLog();
        MarsHQServer server = new MarsHQServer(0, sensorLog);
        Thread serverThread = server.startAsync();

        try {
            waitFor(() -> server.isRunning() && server.getPort() > 0);
            int port = server.getPort();

            try (Socket firstClient = new Socket("localhost", port);
                 PrintWriter firstWriter = new PrintWriter(firstClient.getOutputStream(), true);
                 BufferedReader firstReader = new BufferedReader(new InputStreamReader(firstClient.getInputStream()))) {

                firstWriter.println("TEMP:23.0 °C");
                assertEquals("ACK: TEMP:23.0 °C", firstReader.readLine());
                firstClient.close();
            }

            waitFor(() -> sensorLog.getMessages().stream().anyMatch(message -> message.contains("Client disconnected")));

            try (Socket secondClient = new Socket("localhost", port);
                 PrintWriter writer = new PrintWriter(secondClient.getOutputStream(), true);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(secondClient.getInputStream()))) {

                writer.println("TEMP:24.0");
                assertEquals("ACK: TEMP:24.0 °C", reader.readLine());
            }

            assertTrue(server.isRunning());
        } finally {
            server.stop();
            serverThread.join(2000);
        }
    }

    @Test
    void serverHandlesInvalidMessageFromOneClientAndContinuesWithOthers() throws Exception {
        SensorLog sensorLog = new SensorLog();
        MarsHQServer server = new MarsHQServer(0, sensorLog);
        Thread serverThread = server.startAsync();

        try {
            waitFor(() -> server.isRunning() && server.getPort() > 0);
            int port = server.getPort();

            try (Socket firstClient = new Socket("localhost", port);
                 PrintWriter firstWriter = new PrintWriter(firstClient.getOutputStream(), true);
                 BufferedReader firstReader = new BufferedReader(new InputStreamReader(firstClient.getInputStream()))) {

                firstWriter.println("INVALID:MESSAGE");
                String errorResponse = firstReader.readLine();
                assertTrue(errorResponse.startsWith("[ERROR]"));

                firstWriter.println("TEMP:22.0 °C");
                assertEquals("ACK: TEMP:22.0 °C", firstReader.readLine());
            }

            waitFor(() -> sensorLog.getMessages().stream().anyMatch(message -> message.contains("Client disconnected")));

            try (Socket secondClient = new Socket("localhost", port);
                 PrintWriter writer = new PrintWriter(secondClient.getOutputStream(), true);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(secondClient.getInputStream()))) {

                writer.println("O2:21.0 %");
                assertEquals("ACK: O2:21.0 %", reader.readLine());
            }

            assertTrue(server.isRunning());
            assertTrue(sensorLog.getMessages().stream().anyMatch(message -> message.contains("[ERROR]")));
        } finally {
            server.stop();
            serverThread.join(2000);
        }
    }

    @Test
    void serverHandlesInvalidNumericValueAndContinues() throws Exception {
        SensorLog sensorLog = new SensorLog();
        MarsHQServer server = new MarsHQServer(0, sensorLog);
        Thread serverThread = server.startAsync();

        try {
            waitFor(() -> server.isRunning() && server.getPort() > 0);
            int port = server.getPort();

            try (Socket client = new Socket("localhost", port);
                 PrintWriter writer = new PrintWriter(client.getOutputStream(), true);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()))) {

                writer.println("TEMP:notanumber °C");
                String errorResponse = reader.readLine();
                assertEquals("[ERROR] Invalid numeric value", errorResponse);

                writer.println("TEMP:23.5 °C");
                assertEquals("ACK: TEMP:23.5 °C", reader.readLine());
            }

            assertTrue(sensorLog.getMessages().stream().anyMatch(message -> message.contains("Invalid numeric value")));
        } finally {
            server.stop();
            serverThread.join(2000);
        }
    }

    @Test
    void serverHandlesClientAbruptDisconnect() throws Exception {
        SensorLog sensorLog = new SensorLog();
        MarsHQServer server = new MarsHQServer(0, sensorLog);
        Thread serverThread = server.startAsync();

        try {
            waitFor(() -> server.isRunning() && server.getPort() > 0);
            int port = server.getPort();

            Socket client = new Socket("localhost", port);
            client.close();

            waitFor(() -> sensorLog.getMessages().stream().anyMatch(message -> 
                message.contains("Client disconnected") || message.contains("Connection error")));

            try (Socket secondClient = new Socket("localhost", port);
                 PrintWriter writer = new PrintWriter(secondClient.getOutputStream(), true);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(secondClient.getInputStream()))) {

                writer.println("TEMP:22.0 °C");
                assertEquals("ACK: TEMP:22.0 °C", reader.readLine());
            }

            assertTrue(server.isRunning());
        } finally {
            server.stop();
            serverThread.join(2000);
        }
    }

    private void waitFor(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(25);
        }
        throw new AssertionError("Condition was not met within timeout");
    }

    @FunctionalInterface
    private interface BooleanSupplier {
        boolean getAsBoolean();
    }
}
