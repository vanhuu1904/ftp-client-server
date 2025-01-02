package org.example.ftpclient;

import java.io.*;
import java.net.Socket;

public class CommandHandler {
    private static final int BUFFER_SIZE = 1024;

    // Lệnh LIST: Liệt kê file/thư mục
    public static void listRemote(Socket controlSocket, PrintWriter writer, BufferedReader reader) throws IOException {
        writer.println("PASV");
        String response = reader.readLine();
        System.out.println(response);

        int passivePort = extractPassivePort(response);
        try (Socket dataSocket = new Socket(controlSocket.getInetAddress(), passivePort);
             BufferedReader dataReader = new BufferedReader(new InputStreamReader(dataSocket.getInputStream()))) { // Luồng dữ liệu đầu vào để nhận ds file
            writer.println("LIST");
            System.out.println(readResponse(reader));
            String line;
            while ((line = dataReader.readLine()) != null) {
                System.out.println(line);
            }
            response = reader.readLine();
            System.out.println(response);
        }
    }

    // Lệnh RETR: Tải file từ server
    public static void retrieveFile(Socket controlSocket, PrintWriter writer, BufferedReader reader, String retrFileName) throws IOException {
        writer.println("PASV");
        String response = reader.readLine();
        System.out.println(response);

        int passivePort = extractPassivePort(response);
        try (Socket dataSocket = new Socket(controlSocket.getInetAddress(), passivePort);
             InputStream dataIn = dataSocket.getInputStream()) { // Luồng đầu vào từ datasocket
            String remoteFile = retrFileName;
            writer.println("RETR " + remoteFile);

            response = readResponse(reader);
            System.out.println(response);

            FileOutputStream fileOut = new FileOutputStream(remoteFile);// Luồng đầu ra ghi dữ liệu
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = dataIn.read(buffer)) != -1) {
                fileOut.write(buffer, 0, bytesRead);
            }
            fileOut.close();
            dataIn.close();
            response = reader.readLine();
            System.out.println(response);
        }
    }

    // Lệnh STOR: Tải file lên server
    public static void storeFile(Socket controlSocket, PrintWriter writer, BufferedReader reader, String storFileName) throws IOException {
        writer.println("PASV");
        String response = reader.readLine();
        System.out.println(response);

        int passivePort = extractPassivePort(response);
        try (Socket dataSocket = new Socket(controlSocket.getInetAddress(), passivePort);
             OutputStream dataOut = dataSocket.getOutputStream()) { // Luồng đầu ra
            String localFile = storFileName;
            File file = new File(localFile);
            if (!file.exists() || !file.canRead()) {
                System.out.println("550 File unavailable.");
                return;
            } else {
                writer.println("STOR " + file.getName());
                response = readResponse(reader);
                System.out.println(response);

                FileInputStream fileIn = new FileInputStream(file); // Luồng đầu vào đọc dữ liệu
                byte[] buffer = new byte[BUFFER_SIZE]; // mảng 1kB
                int bytesRead;
                while ((bytesRead = fileIn.read(buffer)) != -1) {
                    dataOut.write(buffer, 0, bytesRead);
                }
                fileIn.close();
                dataOut.close();
                response = readResponse(reader);
                System.out.println(response);
            }
        }
    }

    private static int extractPassivePort(String response) {
        int start = response.indexOf('(');
        int end = response.indexOf(')');
        if (start == -1 || end == -1) return -1;

        String[] parts = response.substring(start + 1, end).split(",");
        int p1 = Integer.parseInt(parts[4]);
        int p2 = Integer.parseInt(parts[5]);
        return p1 * 256 + p2;
    }

    public static void changeWorkingDirectory(Socket controlSocket, PrintWriter writer, BufferedReader reader, String command) throws IOException {
        writer.println(command); // Gửi lệnh CWD
        String response = readResponse(reader); // Đọc phản hồi từ server
        System.out.println(response);
    }
    public static void printWorkingDirectory(Socket controlSocket, PrintWriter writer, BufferedReader reader) throws IOException {
        // Gửi lệnh PWD tới server
        writer.println("PWD");
        // Đọc và in phản hồi từ server
        String response = reader.readLine();
        System.out.println(response);
    }

    // Lệnh DELE: Xóa file trên server
    public static void deleteFile(Socket controlSocket, PrintWriter writer, BufferedReader reader, String fileName) throws IOException {
        writer.println("DELE " + fileName); // Gửi lệnh DELE cùng với tên file
        String response = readResponse(reader); // Đọc phản hồi từ server
        System.out.println(response);
    }
    // Lệnh RMD: Xóa thư mục trên server
    public static void removeDirectory(Socket controlSocket, PrintWriter writer, BufferedReader reader, String directoryName) throws IOException {
        writer.println("RMD " + directoryName); // Gửi lệnh RMD cùng với tên thư mục
        String response = readResponse(reader); // Đọc phản hồi từ server
        System.out.println(response);
//        if (response.startsWith("250")) {
//            System.out.println("Directory removed successfully.");
//        } else {
//            System.out.println("Failed to remove directory: " + response);
//        }
    }

    // Lệnh MKD: Tạo thư mục mới trên server
    public static void makeDirectory(Socket controlSocket, PrintWriter writer, BufferedReader reader, String directoryName) throws IOException {
        writer.println("MKD " + directoryName); // Gửi lệnh MKD cùng với tên thư mục
        String response = readResponse(reader); // Đọc phản hồi từ server
        System.out.println(response);
//        if (response.startsWith("257")) {
//            System.out.println("Directory created successfully.");
//        } else {
//            System.out.println("Failed to create directory: " + response);
//        }
    }
    public static void changeToParentDirectory(Socket controlSocket, PrintWriter writer, BufferedReader reader) {
        try {
            // Gửi lệnh CDUP đến server
            writer.println("CDUP");
            String response = reader.readLine();
            System.out.println(response);

        } catch (IOException e) {
            System.out.println("Error: Unable to execute CDUP command. " + e.getMessage());
        }
    }

    private static String readResponse(BufferedReader reader) throws IOException {
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line).append("\n");
            if (line.matches("^\\d{3} .*")) break; // Dừng khi nhận mã trạng thái FTP
        }
        return response.toString();
    }
}

