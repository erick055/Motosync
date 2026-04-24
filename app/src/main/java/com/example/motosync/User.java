package com.example.motosync;

public class User {
    public String fullName;
    public String email;
    public String role;

    // Default constructor required for Firebase
    public User() {
    }

    public User(String fullName, String email, String role) {
        this.fullName = fullName;
        this.email = email;
        this.role = role;
    }
}