package org.example;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SensorLog {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String LOG_DIRECTORY = "sensorDataLog";
    private static final String LOG_FILE = LOG_DIRECTORY + File.separator + "mars.log";
    private final List<String> messages = new ArrayList<>();

    public SensorLog() {
        File directory = new File(LOG_DIRECTORY);
        if (!directory.exists()) {
            directory.mkdirs();
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

        File logFile = new File(LOG_FILE);
        if (logFile.exists()) {
            logFile.delete();
        }
    }

    private void appendToFile(String message) {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(LOG_FILE, true));
            bufferedWriter.write(message);
            bufferedWriter.newLine();
            bufferedWriter.close();
        } catch (IOException e) {
            throw new IllegalStateException("Could not write to sensor log file", e);
        }
    }
}
