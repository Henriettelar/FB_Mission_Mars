package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class SensorHandler implements Runnable {
    private final Socket clientSocket;
    private final SensorLog sensorLog;

    public SensorHandler(Socket clientSocket, SensorLog sensorLog) {
        this.clientSocket = clientSocket;
        this.sensorLog = sensorLog;
    }

    @Override
    public void run() {
        try (Socket ignored = clientSocket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String clientAddress = clientSocket.getRemoteSocketAddress().toString();
            sensorLog.log("Connected sensor: " + clientAddress);

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                sensorLog.log("Received from " + clientAddress + ": " + line);
                writer.println("ACK: " + line);
            }

            sensorLog.log("Client disconnected: " + clientAddress);
        } catch (IOException e) {
            sensorLog.log("Connection error for client " + clientSocket.getRemoteSocketAddress() + ": " + e.getMessage());
        }
    }
}
