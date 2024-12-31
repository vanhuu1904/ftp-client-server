package org.example.ftpserver;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class CommandHandler {
    private static final Set<String> NON_PRIVILEGED = Set.of("USER", "PASS", "QUIT");
    private static final Set<String> PRIVILEGED = Set.of(
            "SYST", "LIST", "PASV", "CWD", "CDUP", "PWD",
            "RETR", "STOR", "DELE", "RMD", "MKD", "TYPE"
    );

    public static boolean handle(Command cmd, ConnectionHandler conn) {
        String type = cmd.getType();
        String arg = cmd.getArgument();

        // Process commands that do not require a login
        if (NON_PRIVILEGED.contains(type)) {
            switch (type) {
                case "USER":
                    return handleUSER(arg, conn);
                case "PASS":
                    return handlePASS(arg, conn);
                case "QUIT":
                    handleQUIT(conn);
                    return true; // End connection
                default:
                    conn.sendMessage(FTPResponse.NOT_IMPLEMENTED);
            }
            return false;
        }

        // Handle login requests
        if (conn.getCurrentAccount() == null || !conn.getCurrentAccount().isOnline()) {
            conn.sendMessage(FTPResponse.NEED_LOGIN);
            return false;
        }

        switch (type) {
            case "SYST":
                conn.sendMessage(FTPResponse.SYSTEM_INFO);
                break;
            case "LIST":
                handleLIST(conn);
                break;
            case "PASV":
                handlePASV(conn);
                break;
            case "CWD":
                handleCWD(arg, conn);
                break;
            case "CDUP":
                handleCDUP(conn);
                break;
            case "PWD":
                handlePWD(conn);
                break;
            case "RETR":
                handleRETR(arg, conn);
                break;
            case "STOR":
                handleSTOR(arg, conn);
                break;
            case "DELE":
                handleDELE(arg, conn);
                break;
            case "RMD":
                handleRMD(arg, conn);
                break;
            case "MKD":
                handleMKD(arg, conn);
                break;
            case "TYPE":
                handleTYPE(arg, conn);
                break;
            default:
                conn.sendMessage(FTPResponse.NOT_IMPLEMENTED);
        }

        return false;
    }

    // USER
    private static boolean handleUSER(String username, ConnectionHandler conn) {
        if (username == null) {
            conn.sendMessage(FTPResponse.LOGIN_INVALID);
            return false;
        }

        for (Account acc : conn.getAccounts()) {
            if (acc.getUsername().equals(username)) {
                conn.setCurrentAccount(acc);
                conn.sendMessage(FTPResponse.NEED_PASSWORD);
                return false;
            }
        }

        conn.sendMessage(FTPResponse.LOGIN_INVALID);
        return false;
    }

    // PASS
    private static boolean handlePASS(String password, ConnectionHandler conn) {
        Account acc = conn.getCurrentAccount();
        if (acc == null) {
            conn.sendMessage(FTPResponse.BAD_SEQUENCE);
            return false;
        }

        if (password != null && password.equals(acc.getPassword())) {
            acc.setOnline(true);
            String rootFolder = acc.getRootFolder();
            Path projectDir = Paths.get(System.getProperty("user.dir"));
            Path rootPath = projectDir.resolve("src/main/java/org/example/ftpserver").resolve(rootFolder).normalize().toAbsolutePath();
            if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
                conn.sendMessage("550 Root folder does not exist.");
                conn.setCurrentAccount(null);
                return false;
            }
            conn.setWorkingDir(rootPath.toString());
//            System.out.println("workingDir: " + conn.getWorkingDir());
            conn.sendMessage(FTPResponse.LOGIN_SUCCESS);
//            System.out.println("User " + acc.getUsername() + " logged in.");
            return false;
        } else {
            conn.sendMessage(FTPResponse.LOGIN_INVALID);
            conn.setCurrentAccount(null);
            return false;
        }
    }

    // QUIT
    private static void handleQUIT(ConnectionHandler conn) {
        Account acc = conn.getCurrentAccount();
        if (acc != null && acc.isOnline()) {
            acc.setOnline(false);
//            System.out.println("User " + acc.getUsername() + " logged out.");
        }
        conn.sendMessage(FTPResponse.QUIT_SUCCESS);
    }

    // SYST
    private static void handleSYST(ConnectionHandler conn) {
        conn.sendMessage(FTPResponse.SYSTEM_INFO);
    }

    // LIST
    private static void handleLIST(ConnectionHandler conn) {
        try {
            // open data connection
            Socket dataSocket = openDataConnection(conn);
            if (dataSocket == null) {
                return;
            }

            conn.sendMessage("150 Opening data connection.");

            BufferedWriter dataWriter = new BufferedWriter(new OutputStreamWriter(dataSocket.getOutputStream()));

            // Get the current directory path (workingDir)
            Path dirPath = Paths.get(conn.getWorkingDir()).toAbsolutePath();

            // Browse only files/folders in the current working directory
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
                for (Path entry : stream) {
                    // Ignore hidden files/folders
                    if (Files.isHidden(entry)) {
                        continue;
                    }

                    // Get file/folder properties
                    PosixFileAttributes posixAttrs = Files.readAttributes(entry, PosixFileAttributes.class);

                    // Get details
                    String permissions = getPermissions(entry);
                    String owner = posixAttrs.owner().getName();
                    String groupName = posixAttrs.group().getName();
                    long size = posixAttrs.size();
                    String modifiedTime = new SimpleDateFormat("MMM dd HH:mm").format(new Date(posixAttrs.lastModifiedTime().toMillis()));
                    String name = entry.getFileName().toString();

                    // Ghi thông tin vào kết nối dữ liệu
                    String line = String.format("%s 1 %s %s %10d %s %s",
                            permissions, owner, groupName, size, modifiedTime, name);
                    dataWriter.write(line + "\r\n");
                }
            }

            dataWriter.flush();
            dataWriter.close();
            dataSocket.close();

            conn.sendMessage(FTPResponse.CLOSING_DATA_CONN);
        } catch (IOException e) {
            conn.sendMessage(FTPResponse.INTERNAL_ERROR);
            System.err.println("LIST Error: " + e.getMessage());
        }
    }


    // PASV
    private static void handlePASV(ConnectionHandler conn) {
        try {
            ServerSocket serverSocket = new ServerSocket(0);
            conn.setPassiveDataSocket(serverSocket);

            InetAddress localAddress = conn.getDataSocket() != null ? conn.getDataSocket().getLocalAddress() : InetAddress.getLocalHost();
            String ip = localAddress.getHostAddress().replace('.', ',');

            int port = serverSocket.getLocalPort();
            int p1 = port / 256;
            int p2 = port % 256;

            String response = String.format(FTPResponse.ENTERING_PASV, ip, p1, p2);
            conn.sendMessage(response);
        } catch (IOException e) {
            conn.sendMessage(FTPResponse.CANT_OPEN_DATA);
            System.err.println("PASV Error: " + e.getMessage());
        }
    }

    // CWD
    private static void handleCWD(String path, ConnectionHandler conn) {
        if (path == null || path.isEmpty()) {
            conn.sendMessage(FTPResponse.INVALID_PARAMETER);
            return;
        }

        Path newPath = Paths.get(path).normalize();
        if (Files.isDirectory(newPath)) {
            conn.setWorkingDir(newPath.toString());
//            System.out.println("workingDirnew: " + conn.getWorkingDir());
            conn.sendMessage(FTPResponse.COMMAND_OKAY);
        } else {
            conn.sendMessage(FTPResponse.FILE_UNAVAILABLE);
        }
    }

    // CDUP
    private static void handleCDUP(ConnectionHandler conn) {
        Path currentPath = Paths.get(conn.getWorkingDir());
        Path parent = currentPath.getParent();
        if (parent != null) {
            conn.setWorkingDir(parent.toString());
            conn.sendMessage(FTPResponse.COMMAND_OKAY);
        } else {
            conn.sendMessage(FTPResponse.FILE_UNAVAILABLE);
        }
    }

    // PWD
    private static void handlePWD(ConnectionHandler conn) {
        String response = String.format("257 \"%s\" is the current directory.", conn.getWorkingDir());
        conn.sendMessage(response);
    }

    // RETR
    private static void handleRETR(String filename, ConnectionHandler conn) {
        if (filename == null || filename.isEmpty()) {
            conn.sendMessage(FTPResponse.FILE_UNAVAILABLE);
            return;
        }

        Path filePath = Paths.get(conn.getWorkingDir(), filename).normalize();
        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            conn.sendMessage(FTPResponse.FILE_UNAVAILABLE);
            return;
        }

        try {
            Socket dataSocket = openDataConnection(conn);
            if (dataSocket == null) {
                return;
            }

            conn.sendMessage("150 Opening data connection.");

            OutputStream dataOut = dataSocket.getOutputStream();
            Files.copy(filePath, dataOut);
            dataOut.flush();
            dataOut.close();
            dataSocket.close();

            conn.sendMessage(FTPResponse.CLOSING_DATA_CONN);
        } catch (IOException e) {
            conn.sendMessage(FTPResponse.INTERNAL_ERROR);
            System.err.println("RETR Error: " + e.getMessage());
        }
    }

    // STOR
    private static void handleSTOR(String filename, ConnectionHandler conn) {
        if (filename == null || filename.isEmpty()) {
            conn.sendMessage(FTPResponse.FILE_UNAVAILABLE);
            return;
        }

        Path filePath = Paths.get(conn.getWorkingDir(), filename).normalize();

        try {
            Socket dataSocket = openDataConnection(conn);
            if (dataSocket == null) {
                return;
            }

            conn.sendMessage("150 Opening data connection.");

            InputStream dataIn = dataSocket.getInputStream();
            Files.copy(dataIn, filePath, StandardCopyOption.REPLACE_EXISTING);
            dataIn.close();
            dataSocket.close();

            conn.sendMessage(FTPResponse.CLOSING_DATA_CONN);
        } catch (IOException e) {
            conn.sendMessage(FTPResponse.INTERNAL_ERROR);
            System.err.println("STOR Error: " + e.getMessage());
        }
    }

    // DELE
    private static void handleDELE(String filename, ConnectionHandler conn) {
        if (filename == null || filename.isEmpty()) {
            conn.sendMessage(FTPResponse.FILE_UNAVAILABLE);
            return;
        }

        Path filePath = Paths.get(conn.getWorkingDir(), filename).normalize();

        try {
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                conn.sendMessage(FTPResponse.DELETE_SUCCESS);
            } else {
                conn.sendMessage(FTPResponse.FILE_UNAVAILABLE);
            }
        } catch (IOException e) {
            conn.sendMessage(FTPResponse.FILE_UNAVAILABLE);
            System.err.println("DELE Error: " + e.getMessage());
        }
    }

    // RMD
    private static void handleRMD(String dirname, ConnectionHandler conn) {
        if (dirname == null || dirname.isEmpty()) {
            conn.sendMessage(FTPResponse.FILE_UNAVAILABLE);
            return;
        }

        Path dirPath = Paths.get(conn.getWorkingDir(), dirname).normalize();

        try {
            boolean removed = Files.deleteIfExists(dirPath);
            if (removed) {
                conn.sendMessage(FTPResponse.DELETE_SUCCESS);
            } else {
                conn.sendMessage(FTPResponse.FILE_UNAVAILABLE);
            }
        } catch (IOException e) {
            conn.sendMessage(FTPResponse.FILE_UNAVAILABLE);
            System.err.println("RMD Error: " + e.getMessage());
        }
    }

    // MKD
    private static void handleMKD(String dirname, ConnectionHandler conn) {
        if (dirname == null || dirname.isEmpty()) {
            conn.sendMessage(FTPResponse.INVALID_PARAMETER);
            return;
        }

        Path dirPath = Paths.get(conn.getWorkingDir(), dirname).normalize();

        try {
            Files.createDirectories(dirPath);
            conn.sendMessage(FTPResponse.DIRECTORY_CREATED);
        } catch (IOException e) {
            conn.sendMessage(FTPResponse.FILE_UNAVAILABLE);
            System.err.println("MKD Error: " + e.getMessage());
        }
    }

    // TYPE
    private static void handleTYPE(String type, ConnectionHandler conn) {
        if (type == null) {
            conn.sendMessage(FTPResponse.SYNTAX_ERROR);
            return;
        }

        switch (type.toUpperCase()) {
            case "A":
                conn.sendMessage(FTPResponse.TYPE_A_SUCCESS);
                break;
            case "I":
                conn.sendMessage(FTPResponse.TYPE_I_SUCCESS);
                break;
            default:
                conn.sendMessage(FTPResponse.INVALID_PARAMETER);
        }
    }

    // Open data connection
    private static Socket openDataConnection(ConnectionHandler conn) {
        try {
            if (conn.getPassiveDataSocket() != null) {
                // passive mode
                Socket dataSocket = conn.getPassiveDataSocket().accept();
                conn.setPassiveDataSocket(null); // Reset
                return dataSocket;
            } else {
                conn.sendMessage(FTPResponse.CANT_OPEN_DATA);
                return null;
            }
        } catch (IOException e) {
            conn.sendMessage(FTPResponse.CANT_OPEN_DATA);
            System.err.println("Data Connection Error: " + e.getMessage());
            return null;
        }
    }

    // Utility to get permission string
    private static String getPermissions(Path path) {
        StringBuilder sb = new StringBuilder();
        try {
            PosixFileAttributes posixAttrs = Files.readAttributes(path, PosixFileAttributes.class);
            Set<PosixFilePermission> permissions = posixAttrs.permissions();

            // type
            sb.append(posixAttrs.isDirectory() ? "d" : "-");

            // permission owner
            sb.append(permissions.contains(PosixFilePermission.OWNER_READ) ? "r" : "-");
            sb.append(permissions.contains(PosixFilePermission.OWNER_WRITE) ? "w" : "-");
            sb.append(permissions.contains(PosixFilePermission.OWNER_EXECUTE) ? "x" : "-");

            // permission group
            sb.append(permissions.contains(PosixFilePermission.GROUP_READ) ? "r" : "-");
            sb.append(permissions.contains(PosixFilePermission.GROUP_WRITE) ? "w" : "-");
            sb.append(permissions.contains(PosixFilePermission.GROUP_EXECUTE) ? "x" : "-");

            // permission other
            sb.append(permissions.contains(PosixFilePermission.OTHERS_READ) ? "r" : "-");
            sb.append(permissions.contains(PosixFilePermission.OTHERS_WRITE) ? "w" : "-");
            sb.append(permissions.contains(PosixFilePermission.OTHERS_EXECUTE) ? "x" : "-");

        } catch (IOException e) {
            sb.append("----------");
            System.err.println("Error retrieving permissions for " + path + ": " + e.getMessage());
        }

        return sb.toString();
    }
}
