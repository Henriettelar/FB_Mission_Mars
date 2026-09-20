package org.example;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SensorLog {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final List<String> messages = new ArrayList<>();

    public void log(String message) {
        String timestamped = FORMATTER.format(LocalDateTime.now()) + " - " + message;
        synchronized (messages) {
            messages.add(timestamped);
        }
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
    }
}
