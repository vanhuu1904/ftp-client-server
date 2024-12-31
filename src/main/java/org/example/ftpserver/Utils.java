package org.example.ftpserver;


import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Utils {
    public static List<Account> readAccountsFromFile(InputStream inputStream) {
        List<Account> accounts = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 3) {
                    String username = parts[0];
                    String password = parts[1];
                    String rootFolder = parts[2];
                    accounts.add(new Account(username, password, rootFolder));
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading accounts: " + e.getMessage());
        }

        return accounts;
    }
}

