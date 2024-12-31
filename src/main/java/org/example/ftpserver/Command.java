package org.example.ftpserver;


public class Command {
    private String type;
    private String argument;

    public Command(String type, String argument) {
        this.type = type.toUpperCase();
        this.argument = argument;
    }

    public String getType() {
        return type;
    }

    public String getArgument() {
        return argument;
    }
}
