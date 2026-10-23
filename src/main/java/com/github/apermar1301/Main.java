package com.github.apermar1301;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {
        new Main().launchAndVerify();
    }

    public void launchAndVerify() {
        boolean isWindows = isWindows();
        File targetDirectory = getTargetDirectory(isWindows);
        ensureDirectoryExists(targetDirectory);

        String customId = "ID-" + System.currentTimeMillis();
        ProcessBuilder processBuilder = createProcessBuilder(targetDirectory, customId);

        try {
            Process process = processBuilder.start();
            List<String> outputLines = captureProcessOutput(process);
            int exitCode = process.waitFor();

            verifySubprocessOutput(outputLines, customId, targetDirectory, exitCode);
        } catch (IOException e) {
            System.err.println("Execution error: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Process interrupted: " + e.getMessage());
        }
    }

    private ProcessBuilder createProcessBuilder(File targetDirectory, String customId) {
        String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        String rawClasspath = System.getProperty("java.class.path");
        String absoluteClasspath = Arrays.stream(rawClasspath.split(File.pathSeparator))
                .map(path -> new File(path).getAbsolutePath())
                .collect(Collectors.joining(File.pathSeparator));

        ProcessBuilder processBuilder = new ProcessBuilder(
                javaBin,
                "-cp",
                absoluteClasspath,
                ContextInfo.class.getName()
        );
        processBuilder.directory(targetDirectory);
        processBuilder.environment().put("CUSTOM_ID", customId);
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
        return processBuilder;
    }

    private List<String> captureProcessOutput(Process process) throws IOException {
        List<String> outputLines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                outputLines.add(line);
                System.out.println(line);
            }
        }
        return outputLines;
    }

    private void verifySubprocessOutput(List<String> outputLines, String expectedCustomId, File targetDirectory, int exitCode) {
        boolean customIdMatches = !outputLines.isEmpty() && outputLines.get(0).trim().equals(expectedCustomId);

        String capturedDirectory = outputLines.size() > 1 ? outputLines.get(1).trim() : "";
        String expectedAbsolutePath = targetDirectory.getAbsolutePath();
        String expectedCanonicalPath;
        try {
            expectedCanonicalPath = targetDirectory.getCanonicalPath();
        } catch (IOException e) {
            expectedCanonicalPath = expectedAbsolutePath;
        }

        boolean directoryMatches = capturedDirectory.equalsIgnoreCase(expectedAbsolutePath)
                || capturedDirectory.equalsIgnoreCase(expectedCanonicalPath)
                || normalizePath(capturedDirectory).equalsIgnoreCase(normalizePath(expectedAbsolutePath))
                || normalizePath(capturedDirectory).equalsIgnoreCase(normalizePath(expectedCanonicalPath));

        System.out.println("CUSTOM_ID verification: " + (customIdMatches ? "SUCCESS" : "FAILURE"));
        System.out.println("Working directory verification: " + (directoryMatches ? "SUCCESS" : "FAILURE"));
        System.out.println("Exit code: " + exitCode);

        if (!customIdMatches || !directoryMatches || exitCode != 0) {
            System.err.println("Verification failed.");
        }
    }

    private String normalizePath(String path) {
        String normalized = path.replace('\\', '/');
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private File getTargetDirectory(boolean isWindows) {
        return new File(isWindows ? "C:\\Temp" : "/tmp");
    }

    private void ensureDirectoryExists(File directory) {
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    private boolean isWindows() {
        String operatingSystem = System.getProperty("os.name");
        return operatingSystem != null && operatingSystem.toLowerCase(Locale.ROOT).contains("win");
    }
}
