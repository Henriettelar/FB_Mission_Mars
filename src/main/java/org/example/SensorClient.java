package org.example;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Random;
import java.util.Scanner;

public class SensorClient implements AutoCloseable, Runnable {
    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 5000;

    private static final Random RANDOM = new Random();

    private final SensorType sensorType;
    private final String host;
    private final int port;
    private volatile boolean running;
    private Socket socket;
    private PrintWriter writer;

    public SensorClient(SensorType sensorType) {
        this(sensorType, DEFAULT_HOST, DEFAULT_PORT);
    }

    public SensorClient(SensorType sensorType, String host, int port) {
        if (sensorType == null) {
            throw new IllegalArgumentException("sensorType cannot be null");
        }
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("host cannot be blank");
        }
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }

        this.sensorType = sensorType;
        this.host = host;
        this.port = port;
    }

    public SensorType getSensorType() {
        return sensorType;
    }

    public String generateReading() {
        double value = generateValue(sensorType);
        return formatReading(value, sensorType);
    }

    public void connect() throws IOException {
        if (socket != null && !socket.isClosed()) {
            return;
        }

        socket = new Socket(host, port);
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    public void sendReading() throws IOException {
        if (writer == null) {
            connect();
        }
        writer.println(generateReading());
    }

    public void start() throws IOException {
        running = true;
        connect();

        while (running) {
            sendReading();
            try {
                Thread.sleep(5000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
    }

    public void stop() throws IOException {
        running = false;

        if (writer != null) {
            writer.close();
            writer = null;
        }

        if (socket != null && !socket.isClosed()) {
            socket.close();
            socket = null;
        }
    }

    @Override
    public void run() {
        try {
            start();
        } catch (IOException e) {
            throw new RuntimeException("Sensor client failed to communicate with server", e);
        }
    }

    @Override
    public void close() throws IOException {
        stop();
    }

    public static void main(String[] args) throws IOException {
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;

        if (args.length >= 1 && !args[0].isBlank()) {
            host = args[0].trim();
        }
        if (args.length >= 2 && !args[1].isBlank()) {
            port = Integer.parseInt(args[1].trim());
        }

        SensorType sensorType = promptForSensorType();
        SensorClient client = new SensorClient(sensorType, host, port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                client.close();
            } catch (IOException ignored) {
            }
        }));

        System.out.println("Starting sensor client with type " + sensorType.getType() + " on " + host + ":" + port);
        client.start();
    }

    static SensorType resolveSensorTypeChoice(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Sensor type is missing");
        }

        String trimmed = input.trim();
        return switch (trimmed) {
            case "1" -> SensorType.TEMPERATURE;
            case "2" -> SensorType.OXYGEN;
            case "3" -> SensorType.AIR_PRESSURE;
            case "4" -> SensorType.CO2;
            default -> SensorType.fromMessageType(trimmed);
        };
    }

    private static SensorType promptForSensorType() {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("Choose sensor type:");
            System.out.println("1) TEMPERATURE (TEMP)");
            System.out.println("2) OXYGEN (O2)");
            System.out.println("3) AIR_PRESSURE (PRESSURE)");
            System.out.println("4) CO2");
            System.out.print("Enter choice (number or type): ");

            String choice = scanner.nextLine();
            try {
                return resolveSensorTypeChoice(choice);
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid choice: " + e.getMessage());
            }
        }
    }





    public double generateValue(SensorType sensorType) {
        return sensorType.getMinValue() + (sensorType.getMaxValue() - sensorType.getMinValue()) * RANDOM.nextDouble();
    }
    public boolean isInRange(double value, SensorType sensorType) {
        return value >= sensorType.getMinValue() && value <= sensorType.getMaxValue();
    }
    public String formatReading(double value, SensorType sensorType) {
        return sensorType.getType() + ":" + String.format(Locale.US, "%.1f", value) + " " + sensorType.getUnit();
    }

}
