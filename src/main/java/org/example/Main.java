package org.example;

public class Main {
    public static void main(String[] args) throws Exception {
        try (MarsHQServer server = new MarsHQServer(5000)) {
            server.startAsync();
            Thread.sleep(1000);
        }
    }
}