package org.example;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MarsHQServer implements AutoCloseable {
    private static final int DEFAULT_PORT = 5000;
    private static final int THREAD_POOL_SIZE = 5;

    private final int port;
    private final SensorLog sensorLog;
    private final ExecutorService executorService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ServerSocket serverSocket;

    public MarsHQServer(int port) {
        this(port, new SensorLog());
    }

    public MarsHQServer(int port, SensorLog sensorLog) {
        this.port = port;
        this.sensorLog = sensorLog;
        this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    public static void main(String[] args) throws IOException {
        try (MarsHQServer server = new MarsHQServer(DEFAULT_PORT)) {
            server.start();
        }
    }

    public void start() throws IOException {
        if (executorService.isShutdown()) {
            throw new IllegalStateException("Server has been stopped and cannot be restarted.");
        }
        if (running.getAndSet(true)) {
            throw new IllegalStateException("Server is already running.");
        }


        try {
            serverSocket = new ServerSocket(port);
            sensorLog.log("Mars HQ server started on port " + serverSocket.getLocalPort());
            System.out.println("Mars HQ server started on port " + serverSocket.getLocalPort());
            while (running.get()) {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(new SensorHandler(clientSocket, sensorLog));

            }
        } catch (IOException e) {
            if (running.get()) {
                throw e;
            }
        } finally {
            running.set(false);
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        }
    }

    //Til test af start server asynkront
    public Thread startAsync() {
        Thread serverThread = new Thread(() -> {
            try {
                start();
            } catch (IOException e) {
                if (running.get()) {
                    throw new RuntimeException("Failed to start Mars HQ server", e);
                }
            }
        }, "mars-hq-server");
        serverThread.start();
        return serverThread;
    }

    public int getPort() {
        if (serverSocket == null) {
            return port;
        }
        return serverSocket.getLocalPort();
    }

    public boolean isRunning() {
        return running.get();
    }

    public void stop() throws IOException {
        running.set(false);

        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }

        executorService.shutdownNow();
    }

    @Override
    public void close() throws IOException {
        stop();
    }
}
