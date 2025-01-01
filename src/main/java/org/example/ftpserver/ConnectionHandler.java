package org.example.ftpserver;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.List;

public class ConnectionHandler implements Runnable {
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private List<Account> accounts;
    private Account currentAccount;
    private String workingDir;
    private ServerSocket passiveDataSocket;
    private Socket dataSocket;

    public ConnectionHandler(Socket socket, List<Account> accounts) {
        this.socket = socket;
        this.accounts = accounts;
        this.currentAccount = null;
        this.workingDir = "/";
        this.passiveDataSocket = null;
        this.dataSocket = null;

        try {
            reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));
        } catch (IOException e) {
            System.err.println("Connection Handler Error: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            sendMessage(FTPResponse.WELCOME);

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Received: " + line);
                Command command = CommandParser.parse(line);
                if (command == null) {
                    sendMessage(FTPResponse.NOT_IMPLEMENTED);
                    continue;
                }

                boolean shouldTerminate = CommandHandler.handle(command, this);
                if (shouldTerminate) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Connection Error: " + e.getMessage());
        } finally {
            cleanup();
        }
    }


    public void sendMessage(String message) {
        try {
            writer.write(message + "\r\n");
            writer.flush();
//            System.out.println("Sent: " + message);
        } catch (IOException e) {
            System.err.println("Send Message Error: " + e.getMessage());
        }
    }


    public List<Account> getAccounts() {
        return accounts;
    }

    public Account getCurrentAccount() {
        return currentAccount;
    }

    public void setCurrentAccount(Account account) {
        this.currentAccount = account;
    }

    public String getWorkingDir() {
        return workingDir;
    }

    public void setWorkingDir(String dir) {
        this.workingDir = dir;
    }

    public ServerSocket getPassiveDataSocket() {
        return passiveDataSocket;
    }

    public void setPassiveDataSocket(ServerSocket passiveDataSocket) {
        this.passiveDataSocket = passiveDataSocket;
    }

    public Socket getDataSocket() {
        return dataSocket;
    }

    public void setDataSocket(Socket dataSocket) {
        this.dataSocket = dataSocket;
    }

    public Path resolvePath(String relativePath) {
        Path rootPath = Paths.get(workingDir).toAbsolutePath(); // Root folder của user
        Path resolvedPath = rootPath.resolve(relativePath).normalize(); // Chuẩn hóa đường dẫn

        // Đảm bảo resolvedPath nằm trong rootPath
        if (!resolvedPath.startsWith(rootPath)) {
            throw new SecurityException("Access denied: Attempt to access outside root folder.");
        }

        return resolvedPath;
    }


    public void cleanup() {
        try {
            if (currentAccount != null) {
                currentAccount.setOnline(false);
            }
            if (passiveDataSocket != null && !passiveDataSocket.isClosed()) {
                passiveDataSocket.close();
            }
            if (dataSocket != null && !dataSocket.isClosed()) {
                dataSocket.close();
            }
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Cleanup Error: " + e.getMessage());
        }
    }
}
