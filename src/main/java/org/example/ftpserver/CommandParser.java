package org.example.ftpserver;


public class CommandParser {
    public static Command parse(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }

        String[] parts = raw.split(" ", 2);
        String type = parts[0];
        String argument = (parts.length > 1) ? parts[1].trim() : null;

        return new Command(type, argument);
    }
}
