package org.example;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SensorLog {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Path LOG_DIRECTORY = Paths.get("sensorDataLog");
    private static final Path LOG_FILE = LOG_DIRECTORY.resolve("mars.log");
    private final List<String> messages = new ArrayList<>();

    public SensorLog() {
        try {
            Files.createDirectories(LOG_DIRECTORY);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create sensor log directory", e);
        }
    }

    public void log(String message) {
        String timestamped = FORMATTER.format(LocalDateTime.now()) + " - " + message;
        synchronized (messages) {
            messages.add(timestamped);
        }
        appendToFile(timestamped);
    }

    public List<String> getMessages() {
        synchronized (messages) {
            return Collections.unmodifiableList(new ArrayList<>(messages));
        }
    }

    public void clear() {
        synchronized (messages) {
            messages.clear();
        }
        try {
            Files.deleteIfExists(LOG_FILE);
        } catch (IOException e) {
            throw new IllegalStateException("Could not clear sensor log file", e);
        }
    }

    private void appendToFile(String message) {
        try (BufferedWriter writer = Files.newBufferedWriter(
                LOG_FILE,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {
            writer.write(message);
            writer.newLine();
        } catch (IOException e) {
            throw new IllegalStateException("Could not write to sensor log file", e);
        }
    }
}
