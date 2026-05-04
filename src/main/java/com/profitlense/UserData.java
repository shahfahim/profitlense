package com.profitlense;

import java.util.Arrays;

public class UserData {
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String phone;
    private String location;
    private String photoPath;
    private String role;

    public UserData(String email, String password, String firstName, String lastName, String phone, String location, String photoPath, String role) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.location = location;
        this.photoPath = photoPath;
        this.role = (role == null || role.isEmpty()) ? "USER" : role;
    }

    // --- Getters ---
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getPhone() { return phone; }
    public String getLocation() { return location; }
    public String getPhotoPath() { return photoPath; }
    public String getRole() { return role; }

    // --- Setters (for Admin Panel) ---
    public void setRole(String role) { this.role = role; }
    public void setPassword(String password) { this.password = password; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setLocation(String location) { this.location = location; }


    public String toCsvString() {
        return String.join(",",
                email != null ? email : "",
                password != null ? password : "",
                firstName != null ? firstName : "",
                lastName != null ? lastName : "",
                phone != null ? phone : "",
                location != null ? location : "",
                photoPath != null ? photoPath : "",
                role != null ? role : "USER"
        );
    }

    public static UserData fromCsvString(String csvLine) {
        if (csvLine == null || csvLine.trim().isEmpty()) return null;
        String[] parts = csvLine.split(",", 8);
        if (parts.length < 8) {
            String[] paddedParts = Arrays.copyOf(parts, 8);
            for(int i = parts.length; i < 8; i++) {
                paddedParts[i] = (i == 7) ? "USER" : "";
            }
            parts = paddedParts;
        }
        return new UserData(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], parts[6], parts[7]);
    }
}