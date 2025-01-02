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
                    handleUSER(arg, conn);
                    break;
                case "PASS":
                    handlePASS(arg, conn);
                    break;
                case "QUIT":
                    handleQUIT(conn);
                    conn.cleanup();
                    break;
                default:
                    conn.sendMessage(FTPResponse.NOT_IMPLEMENTED);
            }
            return false;
        }

        // Check login before processing commands that require permissions
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
    private static void handleUSER(String username, ConnectionHandler conn) {
        if (username == null) {
            conn.sendMessage(FTPResponse.LOGIN_INVALID);
            return;
        }


        for (Account acc : conn.getAccounts()) {
            if (acc.getUsername().equals(username)) {
                conn.setCurrentAccount(acc);
                conn.sendMessage(FTPResponse.NEED_PASSWORD);
                return;
            }
        }

        conn.sendMessage(FTPResponse.LOGIN_INVALID);
    }

    private static void handlePASS(String password, ConnectionHandler conn) {
        Account acc = conn.getCurrentAccount();
        if (acc == null) {
            conn.sendMessage(FTPResponse.BAD_SEQUENCE);
            return;
        }

        if (password != null && password.equals(acc.getPassword())) {
            acc.setOnline(true);
            Path baseUserDir = Paths.get(System.getProperty("user.dir"))
                    .resolve("src/main/java/org/example/ftpserver/user");

            // Create rootFolder path for user
            Path rootPath = baseUserDir.resolve(acc.getRootFolder()).normalize();

            // Check root directory
            if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
                conn.sendMessage("550 Root folder does not exist.");
                conn.setCurrentAccount(null);
                return;
            }

            conn.setWorkingDir(rootPath.toString() + "/");
            conn.sendMessage(FTPResponse.LOGIN_SUCCESS);
        } else {
            conn.sendMessage(FTPResponse.LOGIN_INVALID);
            conn.setCurrentAccount(null);
        }
    }


    // QUIT
    private static void handleQUIT(ConnectionHandler conn) {
        Account acc = conn.getCurrentAccount();
        if (acc != null && acc.isOnline()) {
            acc.setOnline(false);
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
            // Open data connection
            Socket dataSocket = openDataConnection(conn);
            if (dataSocket == null) {
                return;
            }

            conn.sendMessage("150 Opening data connection.");

            BufferedWriter dataWriter = new BufferedWriter(new OutputStreamWriter(dataSocket.getOutputStream()));

            // Get the current directory path (workingDir)
            Path dirPath = conn.resolvePath(".");

            // Browse only files/folders in the current working directory
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
                for (Path entry : stream) {
                    // Ignore hidden files/folders
                    if (Files.isHidden(entry)) {
                        continue;
                    }

                    // Get file/folder properties
                    BasicFileAttributes basicAttrs = Files.readAttributes(entry, BasicFileAttributes.class);

                    // Get details
                    String permissions = getPermissions(entry);
                    String owner = getOwner(entry);
                    String groupName = "group";
                    long size = basicAttrs.size();
                    String modifiedTime = new SimpleDateFormat("MMM dd HH:mm").format(new Date(basicAttrs.lastModifiedTime().toMillis()));
                    String name = entry.getFileName().toString();

                    // Write information to the data connection
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
//    private static void handleCWD(String path, ConnectionHandler conn) {
//        if (path == null || path.isEmpty()) {
//            conn.sendMessage(FTPResponse.INVALID_PARAMETER);
//            return;
//        }
//
////        Path newPath = conn.resolvePath(path);
//        Path newPath = Paths.get(conn.getWorkingDir(), path).normalize();
//        if (Files.isDirectory(newPath)) {
//            conn.setWorkingDir(newPath.toString());
//            conn.sendMessage(FTPResponse.COMMAND_OKAY);
//        } else {
//            conn.sendMessage(FTPResponse.FILE_UNAVAILABLE);
//        }
//    }
    private static void handleCWD(String path, ConnectionHandler conn) {
        if (path == null || path.isEmpty()) {
            conn.sendMessage(FTPResponse.INVALID_PARAMETER);
            return;
        }

        Path rootPath = conn.getRootPath();
        Path newPath;

        if (path.startsWith("/")) {
            if (path.equals("/" + conn.getCurrentAccount().getRootFolder())) {
                newPath = rootPath;
            } else {
                // Giải quyết đường dẫn tương đối từ rootPath
                newPath = rootPath.resolve(path.substring(1)).normalize();
            }
        } else {
            newPath = Paths.get(conn.getWorkingDir()).resolve(path).normalize();
        }

        if (!newPath.startsWith(rootPath)) {
            conn.sendMessage("550 Access denied.");
            return;
        }

        if (Files.isDirectory(newPath)) {
            conn.setWorkingDir(newPath.toString());
            conn.sendMessage(FTPResponse.COMMAND_OKAY);
        } else {
            conn.sendMessage(FTPResponse.FILE_UNAVAILABLE);
        }
    }


    // CDUP
    private static void handleCDUP(ConnectionHandler conn) {
        Path currentPath = Paths.get(conn.getWorkingDir());
        Path rootPath = conn.getRootPath();
        Path parent = currentPath.getParent();

        if (parent != null && parent.startsWith(rootPath)) {
            conn.setWorkingDir(parent.toString());
            conn.sendMessage(FTPResponse.COMMAND_OKAY);
        } else {
            conn.sendMessage(FTPResponse.FILE_UNAVAILABLE);
        }
    }

    // PWD
    private static void handlePWD(ConnectionHandler conn) {
        Path currentPath = Paths.get(conn.getWorkingDir());
        Path rootPath = conn.getRootPath();

        // Get the path relative to rootPath
        Path relativePath = rootPath.relativize(currentPath);

        String displayPath = relativePath.toString().isEmpty() ? "/" : "/" + relativePath.toString().replace("\\", "/");

        String response = String.format("257 \"%s\" is the current directory.", displayPath);
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

            try (OutputStream dataOut = dataSocket.getOutputStream();
                 FileInputStream fileIn = new FileInputStream(filePath.toFile())) {

                byte[] buffer = new byte[4096]; // 4KB buffer size
                int bytesRead;
                while ((bytesRead = fileIn.read(buffer)) != -1) {
                    dataOut.write(buffer, 0, bytesRead);
                }
                dataOut.flush();
            }
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

            try (InputStream dataIn = dataSocket.getInputStream();
                 FileOutputStream fileOut = new FileOutputStream(filePath.toFile())) {

                byte[] buffer = new byte[4096]; // 4KB buffer size
                int bytesRead;
                while ((bytesRead = dataIn.read(buffer)) != -1) {
                    fileOut.write(buffer, 0, bytesRead);
                }
                fileOut.flush();
            }
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

        Path filePath = conn.resolvePath(filename);

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
        if (Files.isDirectory(path)) {
            sb.append("d");
        } else {
            sb.append("-");
        }

        // Owner permissions
        sb.append(Files.isReadable(path) ? "r" : "-");
        sb.append(Files.isWritable(path) ? "w" : "-");
        sb.append(Files.isExecutable(path) ? "x" : "-");

        sb.append("---");
        sb.append("---");

        return sb.toString();
    }

    private static String getOwner(Path path) {
        try {
            return Files.getOwner(path).getName();
        } catch (IOException e) {
            System.err.println("Error retrieving owner for " + path + ": " + e.getMessage());
            return "unknown";
        }
    }
}
