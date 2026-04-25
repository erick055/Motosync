package com.example.motosync;

public class User {
    public String fullName;
    public String email;
    public String mobileNumber; // <-- NEW
    public String role;

    // Default constructor required for Firebase
    public User() {
    }

    public User(String fullName, String email, String mobileNumber, String role) {
        this.fullName = fullName;
        this.email = email;
        this.mobileNumber = mobileNumber;
        this.role = role;
    }
}