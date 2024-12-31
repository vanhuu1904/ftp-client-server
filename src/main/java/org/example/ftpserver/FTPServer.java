package org.example.ftpserver;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class FTPServer {
    private static final int PORT = 1234;
    private List<Account> accounts;

    public FTPServer() {
        accounts = new ArrayList<>();
        loadAccounts("database.txt");
    }

    private void loadAccounts(String filePath) {
        InputStream is = getClass().getClassLoader().getResourceAsStream(filePath);
        accounts  = Utils.readAccountsFromFile(is);
        System.out.println("Loaded " + accounts.size() + " accounts.");
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("FTP Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted connection from " + clientSocket.getInetAddress());

                ConnectionHandler handler = new ConnectionHandler(clientSocket, accounts);
                Thread thread = new Thread(handler);
                thread.start();
            }
        } catch (IOException e) {
            System.err.println("Server Error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        FTPServer server = new FTPServer();
        server.start();
    }
}
