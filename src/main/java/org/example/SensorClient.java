package org.example;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Random;

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





    public double generateValue(SensorType sensorType) {
        return sensorType.getMinValue() + (sensorType.getMaxValue() - sensorType.getMinValue()) * RANDOM.nextDouble();
    }
    public boolean isInRange(double value, SensorType sensorType) {
        return value >= sensorType.getMinValue() && value <= sensorType.getMaxValue();
    }
    public String formatReading(double value, SensorType sensorType) {
        return sensorType.getType() + ":" + String.format(Locale.US, "%.1f", value);
    }

}
