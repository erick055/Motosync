package com.example.motosync;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SignUpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        LinearLayout btnSignUp = findViewById(R.id.btnGoToSignUp);
        TextView btnGoToSignIn = findViewById(R.id.btnGoToSignIn);

        // When user clicks Sign Up, take them to the Dashboard
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Later, we will add the PHP database saving logic here!
                Toast.makeText(SignUpActivity.this, "Account Created!", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // When user clicks "Sign in" at the bottom, go back to Login screen
        btnGoToSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // We simply finish this activity to return to the LoginActivity that is underneath it
                finish();
            }
        });
    }
}