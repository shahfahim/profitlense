package com.profitlense;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ActivityLogger {

    private static final File activityLogFile = new File("src/main/resources/activity_log.csv");
    private static final String CSV_HEADER = "timestamp,email,action,details";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // --- Write a new log entry ---
    public static synchronized void log(String email, String action, String details) {
        UserActivity log = new UserActivity(LocalDateTime.now(), email, action, details);

        try {
            boolean fileExists = activityLogFile.exists();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(activityLogFile, true))) { // 'true' to append
                if (!fileExists || activityLogFile.length() == 0) {
                    writer.write(CSV_HEADER);
                    writer.newLine();
                }
                writer.write(log.toCsvString());
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error writing to activity log: " + e.getMessage());
        }
    }

    // --- Read all log entries ---
    public static List<UserActivity> readActivityLog() {
        List<UserActivity> activities = new ArrayList<>();
        if (!activityLogFile.exists()) {
            return activities; // Return empty list
        }

        try (BufferedReader br = new BufferedReader(new FileReader(activityLogFile))) {
            String line;
            boolean isHeader = true;
            while ((line = br.readLine()) != null) {
                if (isHeader || line.trim().isEmpty()) { isHeader = false; continue; }
                UserActivity activity = UserActivity.fromCsvString(line);
                if (activity != null) {
                    activities.add(activity);
                }
            }
            Collections.reverse(activities); // Show newest first
        } catch (IOException e) {
            System.err.println("Error reading activity_log.csv: " + e.getMessage());
        }
        return activities;
    }

    // --- Clear the log file ---
    public static void clearLog() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(activityLogFile, false))) { // 'false' to overwrite
            writer.write(CSV_HEADER);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error clearing activity log: " + e.getMessage());
        }
    }

    // --- Inner class to hold log data ---
    // This is the class you had as a separate file.
    // It's now inside ActivityLogger.
    public static class UserActivity {
        private LocalDateTime timestamp;
        private String email;
        private String action;
        private String details;

        public UserActivity(LocalDateTime timestamp, String email, String action, String details) {
            this.timestamp = timestamp;
            this.email = email;
            this.action = action;
            this.details = details;
        }

        public String getTimestamp() { return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")); }
        public LocalDateTime getTimestampObject() { return timestamp; }
        public String getEmail() { return email; }
        public String getAction() { return action; }
        public String getDetails() { return details; }

        public String toCsvString() {
            // Helper to escape commas in details
            String safeDetails = (details != null) ? details.replace(",", ";") : "";
            return String.join(",",
                    timestamp.format(formatter),
                    email,
                    action,
                    safeDetails
            );
        }

        public static UserActivity fromCsvString(String csvLine) {
            if (csvLine == null || csvLine.trim().isEmpty()) return null;
            String[] parts = csvLine.split(",", 4);
            if (parts.length < 4) return null;
            try {
                LocalDateTime ts = LocalDateTime.parse(parts[0], formatter);
                return new UserActivity(ts, parts[1], parts[2], parts[3]);
            } catch (DateTimeParseException e) {
                System.err.println("Could not parse activity log timestamp: " + e.getMessage());
                return null;
            }
        }
    }
}