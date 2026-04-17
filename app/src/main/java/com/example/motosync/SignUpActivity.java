package com.example.motosync;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

// Import Firebase libraries
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignUpActivity extends AppCompatActivity {

    // Declare the database reference
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase Database pointing to the root node
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

                    // 1. Simple Validation
                    if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                        Toast.makeText(SignUpActivity.this, "Please fill out all fields.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 2. Generate a unique random ID for the new user
                    String userId = mDatabase.child("Users").push().getKey();

                    // 3. Create the User object (Defaulting role to 'customer')
                    User newUser = new User(name, email, "customer");

                    // 4. Save to Firebase under Users -> [unique_id]
                    if (userId != null) {
                        mDatabase.child("Users").child(userId).setValue(newUser)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Toast.makeText(SignUpActivity.this, "Account Created Successfully!", Toast.LENGTH_SHORT).show();

                                        // Move to Dashboard
                                        Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                                        startActivity(intent);
                                        finish();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(SignUpActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                    }
                }
            });
        }

        // --- NAVIGATION ---
        if (btnGoToSignIn != null) {
            btnGoToSignIn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish(); // Go back to
                }
            });
        }
    }
}