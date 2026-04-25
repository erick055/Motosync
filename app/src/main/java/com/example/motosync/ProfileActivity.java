package com.example.motosync;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    // UI Elements for Dynamic Data
    private TextView tvProfileName, tvProfileRole, tvProfileEmail, tvProfilePhone;
    private TextView tvSidebarName, tvSidebarRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // 1. Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("Users");

        // 2. Initialize UI Elements
        drawerLayout = findViewById(R.id.drawerLayout);
        ImageView btnMenu = findViewById(R.id.btnMenu);

        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileRole = findViewById(R.id.tvProfileRole);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvProfilePhone = findViewById(R.id.tvProfilePhone);

        tvSidebarName = findViewById(R.id.tvSidebarName);
        tvSidebarRole = findViewById(R.id.tvSidebarRole);

        // Buttons
        LinearLayout btnChangePassword = findViewById(R.id.btnChangePassword);
        LinearLayout btnLogoutPage = findViewById(R.id.btnLogoutPage);
        LinearLayout btnLogoutMenu = findViewById(R.id.btnLogoutMenu);

        // Sidebar Navigation
        LinearLayout navDashboard = findViewById(R.id.navDashboard);
        LinearLayout navBookService = findViewById(R.id.navBookService);
        LinearLayout navMyVehicles = findViewById(R.id.navMyVehicles);
        LinearLayout navMyOrders = findViewById(R.id.navMyOrders);
        LinearLayout navMyInvoices = findViewById(R.id.navMyInvoices);
        LinearLayout navProfile = findViewById(R.id.navProfile);

        // =========================================================
        // --- FETCH DYNAMIC FIREBASE USER DATA ---
        // =========================================================
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            mDatabase.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            if (tvProfileName != null) tvProfileName.setText(user.fullName);
                            if (tvProfileEmail != null) tvProfileEmail.setText(user.email);
                            if (tvProfilePhone != null) {
                                tvProfilePhone.setText(user.mobileNumber != null && !user.mobileNumber.isEmpty() ? user.mobileNumber : "No number provided");
                            }

                            // Capitalize Role
                            String displayRole = "";
                            if (user.role != null && user.role.length() > 0) {
                                displayRole = user.role.substring(0, 1).toUpperCase() + user.role.substring(1);
                                if (tvProfileRole != null) tvProfileRole.setText(displayRole + " Account");
                                if (tvSidebarRole != null) tvSidebarRole.setText(displayRole + " Account");
                            }

                            if (tvSidebarName != null) tvSidebarName.setText(user.fullName);
                        }
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(ProfileActivity.this, "Failed to load data.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            AuthUtils.logoutUser(this);
        }

        // =========================================================
        // --- CLICK LISTENERS & NAVIGATION ---
        // =========================================================

        if (btnMenu != null) btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // --- NEW: PASSWORD POPUP TRIGGER ---
        if (btnChangePassword != null) {
            btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        }

        if (btnLogoutPage != null) {
            btnLogoutPage.setOnClickListener(v -> {
                Toast.makeText(ProfileActivity.this, "Logging out...", Toast.LENGTH_SHORT).show();
                AuthUtils.logoutUser(ProfileActivity.this);
            });
        }

        if (btnLogoutMenu != null) {
            btnLogoutMenu.setOnClickListener(v -> {
                Toast.makeText(ProfileActivity.this, "Logging out...", Toast.LENGTH_SHORT).show();
                AuthUtils.logoutUser(ProfileActivity.this);
            });
        }

        // Sidebar Links
        if (navDashboard != null) navDashboard.setOnClickListener(v -> { startActivity(new Intent(this, MainActivity.class)); finish(); });
        if (navBookService != null) navBookService.setOnClickListener(v -> { startActivity(new Intent(this, BookingActivity.class)); finish(); });
        if (navMyVehicles != null) navMyVehicles.setOnClickListener(v -> { startActivity(new Intent(this, VehiclesActivity.class)); finish(); });
        if (navMyOrders != null) navMyOrders.setOnClickListener(v -> { startActivity(new Intent(this, MyOrdersActivity.class)); finish(); });
        if (navMyInvoices != null) navMyInvoices.setOnClickListener(v -> { startActivity(new Intent(this, InvoicesActivity.class)); finish(); });
        if (navProfile != null) navProfile.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
    }

    // =========================================================
    // --- SECURE CHANGE PASSWORD DIALOG ---
    // =========================================================
    private void showChangePasswordDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_change_password);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        EditText etCurrentPassword = dialog.findViewById(R.id.etCurrentPassword);
        EditText etNewPassword = dialog.findViewById(R.id.etNewPassword);
        EditText etConfirmNewPassword = dialog.findViewById(R.id.etConfirmNewPassword);
        LinearLayout btnSavePassword = dialog.findViewById(R.id.btnSavePassword);
        TextView btnCancel = dialog.findViewById(R.id.btnCancel);

        btnSavePassword.setOnClickListener(v -> {
            String currentPass = etCurrentPassword.getText().toString().trim();
            String newPass = etNewPassword.getText().toString().trim();
            String confirmPass = etConfirmNewPassword.getText().toString().trim();

            if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(ProfileActivity.this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPass.equals(confirmPass)) {
                Toast.makeText(ProfileActivity.this, "New passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPass.length() < 6) {
                Toast.makeText(ProfileActivity.this, "New password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null && user.getEmail() != null) {
                // 1. Re-authenticate the user with their old password
                AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPass);

                Toast.makeText(ProfileActivity.this, "Verifying...", Toast.LENGTH_SHORT).show();

                user.reauthenticate(credential).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // 2. If verified, update the password to the new one
                        user.updatePassword(newPass).addOnCompleteListener(updateTask -> {
                            if (updateTask.isSuccessful()) {
                                Toast.makeText(ProfileActivity.this, "Password Updated Successfully!", Toast.LENGTH_LONG).show();
                                dialog.dismiss();
                            } else {
                                Toast.makeText(ProfileActivity.this, "Failed to update password: " + updateTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        Toast.makeText(ProfileActivity.this, "Incorrect Current Password!", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}