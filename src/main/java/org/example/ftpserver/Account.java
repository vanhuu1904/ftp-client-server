package org.example.ftpserver;

public class Account {
    private String username;
    private String password;
    private String rootFolder;
    private boolean online;

    public Account(String username, String password, String rootFolder) {
        this.username = username;
        this.password = password;
        this.rootFolder = rootFolder;
        this.online = false;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getRootFolder() {
        return rootFolder;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean status) {
        this.online = status;
    }
}
