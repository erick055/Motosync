package com.example.motosync;

public class User {
    public String fullName;
    public String email;
    public String password; // Added password field
    public String role;

    public User() {
        // Required empty constructor for Firebase
    }

    public User(String fullName, String email, String password, String role) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.role = role;
    }
}