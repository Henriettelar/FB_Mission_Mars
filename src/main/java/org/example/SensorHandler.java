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
        String clientAddress = "unknown";
        try (Socket ignored = clientSocket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {

            clientAddress = clientSocket.getRemoteSocketAddress().toString();
            sensorLog.log("Connected sensor: " + clientAddress);
            System.out.println("Sensor connected");

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    String errorMsg = "[ERROR] Empty message";
                    sensorLog.log(errorMsg + " from " + clientAddress);
                    writer.println(errorMsg);
                    continue;
                }

                try {
                    ParsedMessage parsed = parseMessage(line);
                    sensorLog.log("Received DATA from " + clientAddress + ": [" + parsed.sensorType().getType() + ": " + parsed.valueText() + " " + parsed.unit() + "]");

                    boolean inThreshold = checkInThreshold(parsed.sensorType(), parsed.value());
                    if (!inThreshold) {
                        String alarmMessage = buildAlarmMessage(parsed.sensorType(), parsed.valueText(), parsed.unit());
                        sensorLog.log("Threshold alarm from " + clientAddress + ": " + alarmMessage);
                        System.out.println(ANSI_RED + "[ALARM: " + parsed.sensorType().getType() + ": " + parsed.valueText() + " " + parsed.unit() + "]" + ANSI_RESET);
                        writer.println(alarmMessage);
                        continue;
                    }

                    writer.println("ACK: " + parsed.sensorType().getType() + ":" + parsed.valueText() + " " + parsed.unit());
                    System.out.println("[" + ANSI_GREEN + parsed.sensorType().getType() + ANSI_RESET + ": " + parsed.valueText() + " " + parsed.unit() + "]");
                } catch (NumberFormatException e) {
                    String errorMsg = "[ERROR] Invalid numeric value";
                    sensorLog.log(errorMsg + " from " + clientAddress + ": " + line);
                    writer.println(errorMsg);
                } catch (IllegalArgumentException e) {
                    String errorMsg = "[ERROR] " + e.getMessage();
                    sensorLog.log(errorMsg + " from " + clientAddress + ": " + line);
                    writer.println(errorMsg);
                }
            }

            sensorLog.log("Client disconnected: " + clientAddress);
        } catch (IOException e) {
            String errorMsg = "[ERROR] Connection error: " + e.getMessage();
            sensorLog.log(errorMsg + " for client " + clientAddress);
            System.err.println(errorMsg);
        } catch (Exception e) {
            String errorMsg = "[ERROR] Unexpected error: " + e.getMessage();
            sensorLog.log(errorMsg + " for client " + clientAddress);
            System.err.println(errorMsg);
        }
    }

    static String buildAlarmMessage(SensorType sensorType, String valueText, String unit) {
        if (sensorType == null) {
            throw new IllegalArgumentException("Sensor type cannot be null");
        }

        String rangeText = sensorType == SensorType.CO2
                ? "< " + sensorType.getMaxThreshold()
                : sensorType.getMinThreshold() + " - " + sensorType.getMaxThreshold();

        return "ALARM: " + sensorType.getType() + ":" + valueText + " " + unit + " outside allowed range [" + rangeText + "]";
    }

    static boolean checkInThreshold(SensorType sensorType, double value) {
        if (sensorType == null) {
            throw new IllegalArgumentException("Sensor type cannot be null");
        }

        if (sensorType == SensorType.CO2) {
            return value <= sensorType.getMaxThreshold();
        }

        return value >= sensorType.getMinThreshold() && value <= sensorType.getMaxThreshold();
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

    private final String ANSI_RESET = "\u001B[0m";

    private final String ANSI_RED = "\u001B[31m";
    private final String ANSI_GREEN = "\u001B[32m";

}
