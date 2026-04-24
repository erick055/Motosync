package com.example.motosync;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SplashActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize Firebase Auth and Database reference
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("Users");

        // 3000 milliseconds = 3 seconds delay
        int SPLASH_DELAY = 3000;

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                FirebaseUser currentUser = mAuth.getCurrentUser();

                // Check if user is logged in AND has verified their email
                if (currentUser != null && currentUser.isEmailVerified()) {
                    checkUserRoleAndRoute(currentUser.getUid());
                } else {
                    // No valid session found, go to Login screen
                    goToLogin();
                }
            }
        }, SPLASH_DELAY);
    }

    private void checkUserRoleAndRoute(String uid) {
        // Query the database to get the user's role securely
        mDatabase.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);

                    if (user != null) {
                        // Route to correct dashboard based on role
                        if ("admin".equals(user.role)) {
                            startActivity(new Intent(SplashActivity.this, AdminDashboardActivity.class));
                        } else {
                            startActivity(new Intent(SplashActivity.this, MainActivity.class));
                        }
                        finish(); // Prevent going back to splash
                        return;
                    }
                }
                // Fallback if data is missing or corrupted
                goToLogin();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Fallback in case of database error
                goToLogin();
            }
        });
    }

    private void goToLogin() {
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}