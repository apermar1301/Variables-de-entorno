package com.github.apermar1301;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

public class ContextInfo {

    public static void main(String[] args) {
        new ContextInfo().run();
    }

    public void run() {
        printEnvironmentVariable();
        printCurrentWorkingDirectory();
        listDirectoryContents();
    }

    private void printEnvironmentVariable() {
        String customId = System.getenv("CUSTOM_ID");
        System.out.println(customId);
    }

    private void printCurrentWorkingDirectory() {
        List<String> command = isWindows()
                ? List.of("cmd.exe", "/c", "cd")
                : List.of("pwd");
        executeCommand(command);
    }

    private void listDirectoryContents() {
        List<String> command = isWindows()
                ? List.of("cmd.exe", "/c", "dir")
                : List.of("ls");
        executeCommand(command);
    }

    private boolean isWindows() {
        String operatingSystem = System.getProperty("os.name");
        return operatingSystem != null && operatingSystem.toLowerCase(Locale.ROOT).contains("win");
    }

    private void executeCommand(List<String> command) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }
            process.waitFor();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println(e.getMessage());
        }
    }
}
