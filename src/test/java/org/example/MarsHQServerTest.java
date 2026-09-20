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

                writer.println("TEMP=22.1");

                assertEquals("ACK: TEMP=22.1", reader.readLine());
            }

            assertTrue(sensorLog.getMessages().stream().anyMatch(message -> message.contains("Received from")));
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

                firstWriter.println("TEMP=21.0");
                secondWriter.println("PRESSURE=1012");

                assertEquals("ACK: TEMP=21.0", firstReader.readLine());
                assertEquals("ACK: PRESSURE=1012", secondReader.readLine());
            }

            assertTrue(sensorLog.getMessages().stream().filter(message -> message.contains("Received from")).count() >= 2);
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

                firstWriter.println("TEMP=23.0");
                assertEquals("ACK: TEMP=23.0", firstReader.readLine());
                firstClient.close();
            }

            waitFor(() -> sensorLog.getMessages().stream().anyMatch(message -> message.contains("Client disconnected")));

            try (Socket secondClient = new Socket("localhost", port);
                 PrintWriter writer = new PrintWriter(secondClient.getOutputStream(), true);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(secondClient.getInputStream()))) {

                writer.println("TEMP=24.0");
                assertEquals("ACK: TEMP=24.0", reader.readLine());
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
