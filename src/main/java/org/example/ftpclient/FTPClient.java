package org.example.ftpclient;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class FTPClient {
    private static final int BUFFER_SIZE = 1024;
    private static Socket controlSocket;
    private static BufferedReader reader;
    private static PrintWriter writer;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        try {
            while(true) {
                try{
                    // Nhập hostname từ bàn phím
                    System.out.print("Enter hostname: ");
                    String hostname = scanner.nextLine();

                    // Nhập port từ bàn phím
                    System.out.print("Enter port: ");
                    int port = Integer.parseInt(scanner.nextLine());

                    // Hiển thị thông tin đã nhập
                    System.out.println("Connecting to hostname: " + hostname);
                    System.out.println("Using port: " + port);
                    // Kết nối tới server
                    controlSocket = new Socket(hostname, port);
                    reader = new BufferedReader(new InputStreamReader(controlSocket.getInputStream()));
                    writer = new PrintWriter(controlSocket.getOutputStream(), true);

                    // Nhận thông báo chào mừng từ server
                    String response = readResponse();
                    System.out.println("\033[32m" + response + "\033[0m");

                    // Nếu kết nối thành công, thoát khỏi vòng lặp
                    break;
                }catch (Exception e) {
                    System.out.println("\033[31mCould not connect to the specified host and port. Please try again.\033[0m");
                }
            }
            // Login
            authenticate();

            BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in));

            while (true) {
                System.out.println("=== FTP Client Options ===");
                System.out.println("\033[36m1. List files (LIST)\033[0m");
                System.out.println("\033[33m2. Retrieve file (RETR)\033[0m");
                System.out.println("\033[34m3. Store file (STOR)\033[0m");
                System.out.println("\033[36m4. Change working directory (CWD)\033[0m");
                System.out.println("\033[33m5. Print working directory (PWD)\033[0m");
                System.out.println("\033[34m6. Delete File (DELE)\033[0m");
                System.out.println("\033[33m7. Remove Directory (RMD)\033[0m");
                System.out.println("\033[34m8. Make Directory (MKD)\033[0m");
                System.out.println("\033[36m9. Change to Parent Director (CDUP)\033[0m");
                System.out.println("\033[33m10. Quit (QUIT)\033[0m");
                System.out.println("==========================");
                System.out.print("Select an option (1-10): ");
                String choice = consoleInput.readLine();

                switch (choice) {
                    case "1":
                        // LIST command
                        CommandHandler.listRemote(controlSocket, writer, reader);
                        break;

                    case "2":
                        // RETR command
                        System.out.print("Enter the name of the file to retrieve: ");
                        String retrFileName = consoleInput.readLine();
                        CommandHandler.retrieveFile(controlSocket, writer, reader, retrFileName);
                        break;

                    case "3":
                        // STOR command
                        System.out.print("Enter the name of the file to store: ");
                        String storFileName = consoleInput.readLine();
                        CommandHandler.storeFile(controlSocket, writer, reader, storFileName);
                    case "4":
                        // CWD command
                        System.out.print("Enter the directory to change to: ");
                        String newDirectory = consoleInput.readLine();
                        CommandHandler.changeWorkingDirectory(controlSocket, writer, reader, "CWD " + newDirectory);
                        break;

                    case "5":
                        // PWD command
                        CommandHandler.printWorkingDirectory(controlSocket, writer, reader);
                        break;

                    case "6":
                        // DELE command (Delete File)
                        System.out.print("Enter the name of the file to delete: ");
                        String fileToDelete = consoleInput.readLine();
                        CommandHandler.deleteFile(controlSocket, writer, reader, fileToDelete);
                        break;

                    case "7":
                        // RMD command (Remove Directory)
                        System.out.print("Enter the name of the directory to remove: ");
                        String directoryToRemove = consoleInput.readLine();
                        CommandHandler.removeDirectory(controlSocket, writer, reader, directoryToRemove);
                        break;

                    case "8":
                        // MKD command (Make Directory)
                        System.out.print("Enter the name of the new directory to create: ");
                        String newDirectoryName = consoleInput.readLine();
                        CommandHandler.makeDirectory(controlSocket, writer, reader, newDirectoryName);
                        break;

                    case "9":
                        // CDUP command (Change to Parent Directory)
                        CommandHandler.changeToParentDirectory(controlSocket, writer, reader);
                        break;

                    case "10":
                        // QUIT command
                        writer.println("QUIT");
                        System.out.println(readResponse());
//                        System.out.println("Goodbye!");
                        return;


                    default:
                        System.out.println("Invalid option. Please try again.");
                        break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (controlSocket != null) controlSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private static void authenticate() throws IOException {
        BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("Enter username: ");
            String username = consoleInput.readLine();
            writer.println("USER " + username);
            String response = readResponse();

            if (response.startsWith("430")) { // Nếu username sai
                System.out.println("\033[31m" + response + "\033[0m");
                continue; // Quay lại nhập username
            } else { // Username hợp lệ, yêu cầu nhập password
                while (true) {
                    System.out.println("\033[32m" + response + "\033[0m");
                    System.out.print("Enter password: ");
                    String password = consoleInput.readLine();
                    writer.println("PASS " + password);
                    response = readResponse();

                    if (response.startsWith("430")) { // Nếu password sai
                        System.out.println("\033[31m" + response + "\033[0m");
                        break; // Quay lại nhập username
                    } else { // Đăng nhập thành công
                        System.out.println("\033[32m" + response + "\033[0m");
                        return; // Thoát chương trình hoặc tiếp tục các chức năng khác
                    }
                }
            }
        }
    }

    private static String readResponse() throws IOException {
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line).append("\n");
            if (line.matches("^\\d{3} .*")) break; // Dừng khi nhận mã trạng thái FTP
        }
        return response.toString();
    }
}

