package com.example.motosync;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mAuth = FirebaseAuth.getInstance();

        EditText etForgotEmail = findViewById(R.id.etForgotEmail);
        LinearLayout btnSendResetLink = findViewById(R.id.btnSendResetLink);
        TextView btnBackToLogin = findViewById(R.id.btnBackToLogin);

        if (btnSendResetLink != null) {
            btnSendResetLink.setOnClickListener(v -> {
                String email = etForgotEmail.getText().toString().trim();

                if (email.isEmpty()) {
                    etForgotEmail.setError("Email is required!");
                    etForgotEmail.requestFocus();
                    return;
                }

                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    etForgotEmail.setError("Please provide a valid email!");
                    etForgotEmail.requestFocus();
                    return;
                }

                Toast.makeText(ForgotPasswordActivity.this, "Sending email...", Toast.LENGTH_SHORT).show();

                // Fire the Firebase password reset trigger!
                mAuth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(ForgotPasswordActivity.this, "Reset link sent! Check your inbox.", Toast.LENGTH_LONG).show();
                            finish(); // Send them back to Login
                        } else {
                            Toast.makeText(ForgotPasswordActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
            });
        }

        // Send user back to the login page if they tap "Back to login"
        if (btnBackToLogin != null) {
            btnBackToLogin.setOnClickListener(v -> finish());
        }
    }
}