package com.example.motosync;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

public class ProfileActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        drawerLayout = findViewById(R.id.drawerLayout);
        ImageView btnMenu = findViewById(R.id.btnMenu);

        // Page Action Buttons
        LinearLayout btnChangePassword = findViewById(R.id.btnChangePassword);
        LinearLayout btnLogoutPage = findViewById(R.id.btnLogoutPage);

        // Sidebar Navigation IDs
        LinearLayout navDashboard = findViewById(R.id.navDashboard);
        LinearLayout navBookService = findViewById(R.id.navBookService);
        LinearLayout navMyVehicles = findViewById(R.id.navMyVehicles);
        LinearLayout navMyOrders = findViewById(R.id.navMyOrders);
        LinearLayout navMyInvoices = findViewById(R.id.navMyInvoices);
        LinearLayout navProfile = findViewById(R.id.navProfile);
        LinearLayout btnLogoutMenu = findViewById(R.id.btnLogoutMenu);

        // --- FETCH AND DISPLAY USER DATA ---
        android.content.SharedPreferences prefs = getSharedPreferences("MotoSyncPrefs", MODE_PRIVATE);
        String savedName = prefs.getString("FULL_NAME", "Customer");
        String savedRole = prefs.getString("ROLE", "motosync");

        TextView tvSidebarName = findViewById(R.id.tvSidebarName);
        TextView tvSidebarRole = findViewById(R.id.tvSidebarRole);

        if (tvSidebarName != null) {
            tvSidebarName.setText(savedName);
        }

        // Optional: Capitalize the first letter of the role (e.g., "customer" -> "Customer")
        if (tvSidebarRole != null && savedRole.length() > 0) {
            String displayRole = savedRole.substring(0, 1).toUpperCase() + savedRole.substring(1);
            tvSidebarRole.setText(displayRole + " Account");
        }
        // -----------------------------------

        // Open Menu
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }

        // --- PAGE BUTTON CLICKS ---
        if (btnChangePassword != null) {
            btnChangePassword.setOnClickListener(v -> Toast.makeText(ProfileActivity.this, "Opening Change Password Form...", Toast.LENGTH_SHORT).show());
        }

        // Handle Logout from the Page Button
        if (btnLogoutPage != null) {
            btnLogoutPage.setOnClickListener(v -> executeLogout());
        }

        // Handle Logout from the Sidebar Menu
        if (btnLogoutMenu != null) {
            btnLogoutMenu.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                executeLogout();
            });
        }

        // --- UNIVERSAL SIDEBAR CLICKS ---
        if (navDashboard != null) {
            navDashboard.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(this, MainActivity.class));
                finish();
            });
        }

        if (navBookService != null) {
            navBookService.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(this, BookingActivity.class));
                finish();
            });
        }

        if (navMyVehicles != null) {
            navMyVehicles.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(this, VehiclesActivity.class));
                finish();
            });
        }

        if (navMyOrders != null) {
            navMyOrders.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(this, MyOrdersActivity.class));
                finish();
            });
        }

        if (navMyInvoices != null) {
            navMyInvoices.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(this, InvoicesActivity.class));
                finish();
            });
        }

        // Already on Profile, just close the menu
        if (navProfile != null) {
            navProfile.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
        }
    }

    // A helper method to handle routing the user to the login screen
    private void executeLogout() {
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}