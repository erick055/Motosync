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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private String currentLoginType = "customer"; // Tracks the active tab

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mDatabase = FirebaseDatabase.getInstance().getReference("Users");

        // UI Elements
        EditText etLoginEmail = findViewById(R.id.etEmail);
        EditText etLoginPassword = findViewById(R.id.etPassword);
        LinearLayout btnLogin = findViewById(R.id.btnSignIn);
        TextView btnGoToSignUp = findViewById(R.id.btnGoToSignUp);

        // Tabs
        TextView tabCustomer = findViewById(R.id.tabCustomer);
        TextView tabAdmin = findViewById(R.id.tabAdmin);

        // --- TAB CLICKS ---
        if (tabCustomer != null && tabAdmin != null) {
            tabCustomer.setOnClickListener(v -> {
                currentLoginType = "customer";
                // Set Customer Active
                tabCustomer.setBackgroundResource(R.drawable.bg_button_primary);
                tabCustomer.setTextColor(getResources().getColor(R.color.bg_dark));
                // Set Admin Inactive
                tabAdmin.setBackgroundResource(android.R.color.transparent);
                tabAdmin.setTextColor(getResources().getColor(R.color.text_secondary));
            });

            tabAdmin.setOnClickListener(v -> {
                currentLoginType = "admin";
                // Set Admin Active
                tabAdmin.setBackgroundResource(R.drawable.bg_button_primary);
                tabAdmin.setTextColor(getResources().getColor(R.color.bg_dark));
                // Set Customer Inactive
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

                Query query = mDatabase.orderByChild("email").equalTo(email);
                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                                User user = userSnapshot.getValue(User.class);

                                if (user != null && user.password.equals(password)) {

                                    // SECURITY CHECK: Ensure they are using the right tab!
                                    if (!user.role.equals(currentLoginType)) {
                                        Toast.makeText(LoginActivity.this, "Access Denied: Please use the " + user.role + " tab.", Toast.LENGTH_LONG).show();
                                        return;
                                    }

                                    Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();

                                    if ("admin".equals(user.role)) {
                                        startActivity(new Intent(LoginActivity.this, AdminDashboardActivity.class));
                                    } else {
                                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                    }
                                    finish();
                                } else {
                                    Toast.makeText(LoginActivity.this, "Incorrect Password.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "No account found.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(LoginActivity.this, "Database Error", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }

        if (btnGoToSignUp != null) {
            btnGoToSignUp.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, SignUpActivity.class)));
        }
    }
}