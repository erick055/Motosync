package com.example.motosync;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private String currentLoginType = "customer"; // Tracks the active tab

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Auth and Database
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("Users");

        // UI Elements
        EditText etLoginEmail = findViewById(R.id.etEmail);
        EditText etLoginPassword = findViewById(R.id.etPassword);
        LinearLayout btnLogin = findViewById(R.id.btnSignIn);
        TextView btnGoToSignUp = findViewById(R.id.btnGoToSignUp);
        TextView btnForgotPassword = findViewById(R.id.btnForgotPassword);

        // Tabs
        TextView tabCustomer = findViewById(R.id.tabCustomer);
        TextView tabAdmin = findViewById(R.id.tabAdmin);

        // --- TAB CLICKS ---
        if (tabCustomer != null && tabAdmin != null) {
            tabCustomer.setOnClickListener(v -> {
                currentLoginType = "customer";
                tabCustomer.setBackgroundResource(R.drawable.bg_button_primary);
                tabCustomer.setTextColor(getResources().getColor(R.color.bg_dark));
                tabAdmin.setBackgroundResource(android.R.color.transparent);
                tabAdmin.setTextColor(getResources().getColor(R.color.text_secondary));
            });

            tabAdmin.setOnClickListener(v -> {
                currentLoginType = "admin";
                tabAdmin.setBackgroundResource(R.drawable.bg_button_primary);
                tabAdmin.setTextColor(getResources().getColor(R.color.bg_dark));
                tabCustomer.setBackgroundResource(android.R.color.transparent);
                tabCustomer.setTextColor(getResources().getColor(R.color.text_secondary));
            });
        }

        // --- LOGIN LOGIC ---
        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> {
                String email = etLoginEmail.getText().toString().trim();
                String password = etLoginPassword.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(LoginActivity.this, "Authenticating...", Toast.LENGTH_SHORT).show();

                // Authenticate using FirebaseAuth
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    FirebaseUser firebaseUser = mAuth.getCurrentUser();

                                    if (firebaseUser != null) {
                                        // Check if the user has verified their email
                                        if (firebaseUser.isEmailVerified()) {
                                            // Fetch their role and data from Realtime Database using their UID
                                            fetchUserDataAndRoute(firebaseUser.getUid());
                                        } else {
                                            Toast.makeText(LoginActivity.this, "Please verify your email address first.", Toast.LENGTH_LONG).show();
                                            mAuth.signOut(); // Sign them out until verified
                                        }
                                    }
                                } else {
                                    Toast.makeText(LoginActivity.this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            });
        }

        if (btnGoToSignUp != null) {
            btnGoToSignUp.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, SignUpActivity.class)));
        }

        if (btnForgotPassword != null) {
            btnForgotPassword.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class)));
        }

    }

    // Helper method to pull the remaining user data (like role) from Realtime DB
    private void fetchUserDataAndRoute(String userId) {
        mDatabase.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    User user = dataSnapshot.getValue(User.class);

                    if (user != null && user.role != null) {

                        // Clean the role string (forces lowercase and removes accidental spaces)
                        String cleanRole = user.role.trim().toLowerCase();

                        // SECURITY CHECK: Ensure they are using the right tab
                        if (!cleanRole.equals(currentLoginType)) {
                            Toast.makeText(LoginActivity.this, "Access Denied: Please use the " + cleanRole + " tab.", Toast.LENGTH_LONG).show();
                            mAuth.signOut(); // Kick them out if trying to bypass roles
                            return;
                        }

                        // Save data to SharedPreferences
                        SharedPreferences prefs = getSharedPreferences("MotoSyncPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("FULL_NAME", user.fullName);
                        editor.putString("EMAIL", user.email);
                        editor.putString("ROLE", cleanRole);
                        editor.apply();

                        Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();

                        // Route to correct dashboard
                        if ("admin".equals(cleanRole)) {
                            startActivity(new Intent(LoginActivity.this, AdminDashboardActivity.class));
                        } else {
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        }
                        finish();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "User data not found in database.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(LoginActivity.this, "Database Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}