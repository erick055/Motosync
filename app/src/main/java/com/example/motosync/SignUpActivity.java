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
    private FirebaseAuth mAuth; // Declare FirebaseAuth

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase Auth and Database pointing to the root node
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Find UI Elements
        EditText etFullName = findViewById(R.id.etFullName);
        EditText etSignUpEmail = findViewById(R.id.etSignUpEmail);
        EditText etSignUpPassword = findViewById(R.id.etSignUpPassword);

        LinearLayout btnSignUp = findViewById(R.id.btnGoToSignUp);
        TextView btnGoToSignIn = findViewById(R.id.btnGoToSignIn);

        // --- SIGN UP LOGIC ---
        if (btnSignUp != null) {
            btnSignUp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String name = etFullName.getText().toString().trim();
                    String email = etSignUpEmail.getText().toString().trim();
                    String password = etSignUpPassword.getText().toString().trim();

                    // 1. Validation
                    if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                        Toast.makeText(SignUpActivity.this, "Please fill out all fields.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        etSignUpEmail.setError("Please provide a valid email");
                        etSignUpEmail.requestFocus();
                        return;
                    }

                    if (password.length() < 6) {
                        etSignUpPassword.setError("Password must be at least 6 characters");
                        etSignUpPassword.requestFocus();
                        return;
                    }

                    Toast.makeText(SignUpActivity.this, "Registering...", Toast.LENGTH_SHORT).show();

                    // 2. Register with hardcoded "customer" role for security
                    registerUser(name, email, password, "customer");
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

    private void registerUser(String name, String email, String password, String role) {
        // 3. Create User in Firebase Authentication (Secures the password)
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();

                            if (firebaseUser != null) {
                                // 4. Send Verification Email
                                firebaseUser.sendEmailVerification();

                                // 5. Save extra user data in Realtime Database using Auth UID
                                String userId = firebaseUser.getUid();

                                // Create the User object (Make sure password is removed from User.java)
                                User newUser = new User(name, email, role);

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