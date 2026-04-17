package com.example.motosync;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Find the Drawer Layout and the Menu Icon
        drawerLayout = findViewById(R.id.drawerLayout);
        ImageView btnMenu = findViewById(R.id.btnMenu);
        // --- FETCH AND DISPLAY USER DATA ---
        android.content.SharedPreferences prefs = getSharedPreferences("MotoSyncPrefs", MODE_PRIVATE);
        String savedName = prefs.getString("FULL_NAME", "Customer");
        String savedRole = prefs.getString("ROLE", "motosync");

        // Find the TextViews
        android.widget.TextView tvSidebarName = findViewById(R.id.tvSidebarName);
        android.widget.TextView tvSidebarRole = findViewById(R.id.tvSidebarRole);
        android.widget.TextView tvWelcomeMessage = findViewById(R.id.tvWelcomeMessage);

        // 1. Set the Full Name in the Sidebar
        if (tvSidebarName != null) {
            tvSidebarName.setText(savedName);
        }

        // 2. Set the Role in the Sidebar (Capitalize the first letter)
        if (tvSidebarRole != null && savedRole.length() > 0) {
            String displayRole = savedRole.substring(0, 1).toUpperCase() + savedRole.substring(1);
            tvSidebarRole.setText(displayRole + " Account");
        }

        // 3. Set the "Welcome back" message using only the First Name
        if (tvWelcomeMessage != null) {
            String firstName = savedName;
            // If the name contains a space, cut it off at the first space
            if (savedName.contains(" ")) {
                firstName = savedName.substring(0, savedName.indexOf(" "));
            }
            tvWelcomeMessage.setText("Welcome back, " + firstName);
        }
        // -----------------------------------
        // 2. Find the Sidebar Buttons
        LinearLayout navDashboard = findViewById(R.id.navDashboard);
        LinearLayout navBookService = findViewById(R.id.navBookService);
        LinearLayout navMyVehicles = findViewById(R.id.navMyVehicles);
        LinearLayout navMyOrders = findViewById(R.id.navMyOrders);
        LinearLayout navMyInvoices = findViewById(R.id.navMyInvoices);
        LinearLayout btnLogoutMenu = findViewById(R.id.btnLogoutMenu);
        LinearLayout navProfile = findViewById(R.id.navProfile);


        // 3. Add a click listener to the Menu Icon
        btnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        // --- MENU BUTTON CLICKS ---

        if (navDashboard != null) {
            navDashboard.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                // Already in MainActivity, no need to start it again
            });
        }
        if (btnLogoutMenu != null) {
            btnLogoutMenu.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                Toast.makeText(MainActivity.this, "Logging out...", Toast.LENGTH_SHORT).show();

                // Go back to Login Page
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clears the back history
                startActivity(intent);
                finish();
            });
        }

        if (navBookService != null) {
            navBookService.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(this, BookingActivity.class));
            });
        }

        if (navMyVehicles != null) {
            navMyVehicles.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(this, VehiclesActivity.class));
            });
        }

        if (navMyOrders != null) {
            navMyOrders.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(this, MyOrdersActivity.class));
            });
        }

        if (navMyInvoices != null) {
            navMyInvoices.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(this, InvoicesActivity.class));
            });
        }
        if (navProfile != null) {
            navProfile.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(this, ProfileActivity.class));
            });
        }

    }
}
