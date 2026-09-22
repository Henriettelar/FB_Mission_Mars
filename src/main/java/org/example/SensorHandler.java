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
                    sensorLog.log("Invalid message from " + clientAddress + ": empty message");
                    writer.println("ERROR: Empty message");
                    continue;
                }

                try {
                    ParsedMessage parsed = parseMessage(line);
                    sensorLog.log("Received from " + clientAddress + ": type=" + parsed.sensorType().getType() + ", value=" + parsed.value() + " " + parsed.unit());
                    writer.println("ACK: " + parsed.sensorType().getType() + ":" + parsed.valueText() + " " + parsed.unit());
                    System.out.println("[" + parsed.sensorType().getType() + ": " + parsed.valueText() + " " + parsed.unit() + "]");
                } catch (NumberFormatException e) {
                    sensorLog.log("Invalid numeric value from " + clientAddress + ": " + line);
                    writer.println("ERROR: Invalid numeric value");
                } catch (IllegalArgumentException e) {
                    sensorLog.log("Invalid message from " + clientAddress + ": " + line + " (" + e.getMessage() + ")");
                    writer.println("ERROR: " + e.getMessage());
                }
            }

            sensorLog.log("Client disconnected: " + clientAddress);
        } catch (IOException e) {
            sensorLog.log("Connection error for client " + clientSocket.getRemoteSocketAddress() + ": " + e.getMessage());
        }
    }

    static ParsedMessage parseMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message cannot be blank");
        }

        String trimmed = message.trim();
        int separatorIndex = trimmed.indexOf(':');
        if (separatorIndex <= 0 || separatorIndex == trimmed.length() - 1 || trimmed.indexOf(':', separatorIndex + 1) != -1) {
            throw new IllegalArgumentException("Message must match TYPE:VALUE");
        }

        String typePart = trimmed.substring(0, separatorIndex).trim();
        String valuePart = trimmed.substring(separatorIndex + 1).trim();
        if (valuePart.isEmpty()) {
            throw new IllegalArgumentException("Message must contain a value");
        }

        String[] tokens = valuePart.split("\\s+");
        if (tokens.length > 2) {
            throw new IllegalArgumentException("Message must match TYPE:VALUE [UNIT]");
        }

        String numericPart = tokens[0];
        String unitPart = tokens.length > 1 ? tokens[1] : "";

        SensorType sensorType = SensorType.fromMessageType(typePart);
        double value = Double.parseDouble(numericPart);
        String unit = unitPart.isEmpty() ? sensorType.getUnit() : unitPart;

        if (!unitPart.isEmpty() && !sensorType.getUnit().equals(unitPart)) {
            throw new IllegalArgumentException("Wrong unit for sensor type");
        }

        return new ParsedMessage(sensorType, value, numericPart, unit);
    }

    record ParsedMessage(SensorType sensorType, double value, String valueText, String unit) {
    }
}
