package com.example.motosync;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

// Import Firebase Auth and Database libraries
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignUpActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase Auth and Database
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // --- NEW: Find the separated First and Last Name elements ---
        EditText etFirstName = findViewById(R.id.etFirstName);
        EditText etLastName = findViewById(R.id.etLastName);

        EditText etSignUpEmail = findViewById(R.id.etSignUpEmail);
        EditText etMobileNumber = findViewById(R.id.etMobileNumber);
        EditText etSignUpPassword = findViewById(R.id.etSignUpPassword);
        EditText etConfirmPassword = findViewById(R.id.etConfirmPassword);

        LinearLayout btnSignUp = findViewById(R.id.btnGoToSignUp);
        TextView btnGoToSignIn = findViewById(R.id.btnGoToSignIn);

        // --- SIGN UP LOGIC ---
        if (btnSignUp != null) {
            btnSignUp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    // Pull the separated names
                    String fName = etFirstName.getText().toString().trim();
                    String lName = etLastName.getText().toString().trim();

                    String email = etSignUpEmail.getText().toString().trim();
                    String mobile = etMobileNumber.getText().toString().trim();
                    String password = etSignUpPassword.getText().toString().trim();
                    String confirmPassword = etConfirmPassword.getText().toString().trim();

                    // 1. Validation for empty fields
                    if (fName.isEmpty() || lName.isEmpty() || email.isEmpty() || mobile.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                        Toast.makeText(SignUpActivity.this, "Please fill out all fields.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        etSignUpEmail.setError("Please provide a valid email");
                        etSignUpEmail.requestFocus();
                        return;
                    }

                    // Strong Password Validation
                    String passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!_]).{8,}$";
                    if (!password.matches(passwordPattern)) {
                        etSignUpPassword.setError("Password must be at least 8 chars, contain an uppercase, a number, and a special character");
                        etSignUpPassword.requestFocus();
                        return;
                    }

                    // 2. Validate Confirm Password matches
                    if (!password.equals(confirmPassword)) {
                        etConfirmPassword.setError("Passwords do not match");
                        etConfirmPassword.requestFocus();
                        return;
                    }

                    Toast.makeText(SignUpActivity.this, "Registering...", Toast.LENGTH_SHORT).show();

                    // --- THE TRICK: Combine them right before saving to keep your database structure intact! ---
                    String combinedFullName = fName + " " + lName;

                    // Pass the combined name string to the database
                    registerUser(combinedFullName, email, mobile, password, "customer");
                }
            });
        }

        // --- NAVIGATION ---
        if (btnGoToSignIn != null) {
            btnGoToSignIn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish(); // Go back to login screen
                }
            });
        }
    }

    private void registerUser(String name, String email, String mobile, String password, String role) {
        // 4. Create User in Firebase Authentication (Secures the password)
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();

                            if (firebaseUser != null) {
                                // 5. Send Verification Email
                                firebaseUser.sendEmailVerification();

                                // 6. Save extra user data in Realtime Database using Auth UID
                                String userId = firebaseUser.getUid();

                                // Create the User object including the combined Full Name
                                User newUser = new User(name, email, mobile, role);

                                mDatabase.child("Users").child(userId).setValue(newUser)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    Toast.makeText(SignUpActivity.this,
                                                            "Account Created! Please check your email to verify.",
                                                            Toast.LENGTH_LONG).show();

                                                    // Sign out so they are forced to log in and verify their email
                                                    mAuth.signOut();
                                                    finish();
                                                } else {
                                                    Toast.makeText(SignUpActivity.this,
                                                            "Failed to save user data.", Toast.LENGTH_LONG).show();
                                                }
                                            }
                                        });
                            }
                        } else {
                            Toast.makeText(SignUpActivity.this,
                                    "Registration Error: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}