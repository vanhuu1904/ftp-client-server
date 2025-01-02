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
                    System.out.println(readResponse());

                    // Nếu kết nối thành công, thoát khỏi vòng lặp
                    break;
                }catch (Exception e) {
                    System.out.println("Could not connect to the specified host and port. Please try again.");
                }
            }
            // Login
            authenticate();

            BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in));

            while (true) {
                System.out.println("=== FTP Client Options ===");
                System.out.println("1. List files (LIST)");
                System.out.println("2. Retrieve file (RETR)");
                System.out.println("3. Store file (STOR)");
                System.out.println("4. Change working directory (CWD)");
                System.out.println("5. Print working directory (PWD)");
                System.out.println("6. Delete File (DELE)");
                System.out.println("7. Remove Directory (RMD)");
                System.out.println("8. Make Directory (MKD)");
                System.out.println("9. Change to Parent Director (CDUP)");
                System.out.println("10. Quit (QUIT)");
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
                        break;

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
            System.out.println(response);

            if (response.startsWith("430")) { // Nếu username sai
                continue; // Quay lại nhập username
            } else { // Username hợp lệ, yêu cầu nhập password
                while (true) {
                    System.out.print("Enter password: ");
                    String password = consoleInput.readLine();
                    writer.println("PASS " + password);
                    response = readResponse();
                    System.out.println(response);

                    if (response.startsWith("430")) { // Nếu password sai
                        break; // Quay lại nhập username
                    } else { // Đăng nhập thành công
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

