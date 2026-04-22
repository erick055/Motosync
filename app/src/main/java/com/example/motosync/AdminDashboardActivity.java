package com.example.motosync;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

public class AdminDashboardActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        drawerLayout = findViewById(R.id.drawerLayout);
        ImageView btnMenu = findViewById(R.id.btnMenu);

        // Open Menu Safely
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }

        // Quick Action Buttons
        LinearLayout btnApproveOrder = findViewById(R.id.btnApproveOrder);
        LinearLayout btnDeclineOrder = findViewById(R.id.btnDeclineOrder);

        if (btnApproveOrder != null) {
            btnApproveOrder.setOnClickListener(v -> Toast.makeText(this, "Order Approved!", Toast.LENGTH_SHORT).show());
        }
        if (btnDeclineOrder != null) {
            btnDeclineOrder.setOnClickListener(v -> Toast.makeText(this, "Order Declined", Toast.LENGTH_SHORT).show());
        }

        setupSidebarSafe();
    }

    private void setupSidebarSafe() {
        // Grab all drawer layout IDs
        LinearLayout navAdminDashboard = findViewById(R.id.navAdminDashboard);
        LinearLayout navManageBookings = findViewById(R.id.navManageBookings);
        LinearLayout navJobOrders = findViewById(R.id.navJobOrders);
        LinearLayout navManageCustomers = findViewById(R.id.navManageCustomers);
        LinearLayout navManageServices = findViewById(R.id.navManageServices);
        LinearLayout btnLogoutMenu = findViewById(R.id.btnLogoutMenu);

        // --- SAFE SIDEBAR NAVIGATION LOGIC ---

        // If we are already on the Dashboard, just slide the drawer closed
        if (navAdminDashboard != null) {
            navAdminDashboard.setOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
            });
        }

        if (navManageBookings != null) {
            navManageBookings.setOnClickListener(v -> {
                startActivity(new Intent(this, AdminAppointmentsActivity.class));
                finish();
            });
        }

        if (navJobOrders != null) {
            navJobOrders.setOnClickListener(v -> {
                startActivity(new Intent(this, AdminJobOrderActivity.class));
                finish();
            });
        }

        if (navManageCustomers != null) {
            navManageCustomers.setOnClickListener(v -> {
                startActivity(new Intent(this, AdminCustomersActivity.class));
                finish();
            });
        }

        // This is your new Inventory connection!
        if (navManageServices != null) {
            navManageServices.setOnClickListener(v -> {
                startActivity(new Intent(this, AdminInventoryActivity.class));
                finish();
            });
        }

        if (btnLogoutMenu != null) {
            btnLogoutMenu.setOnClickListener(v -> {
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        // --- SAFE USER PROFILE INJECTION ---
        SharedPreferences prefs = getSharedPreferences("MotoSyncPrefs", MODE_PRIVATE);
        String savedName = prefs.getString("FULL_NAME", "Admin Name");
        String savedRole = prefs.getString("ROLE", "admin");

        TextView tvSidebarName = findViewById(R.id.tvSidebarName);
        TextView tvSidebarRole = findViewById(R.id.tvSidebarRole);

        if (tvSidebarName != null && savedName != null) {
            tvSidebarName.setText(savedName);
        }
        if (tvSidebarRole != null && savedRole != null && savedRole.length() > 0) {
            String displayRole = savedRole.substring(0, 1).toUpperCase() + savedRole.substring(1);
            tvSidebarRole.setText(displayRole + " Account");
        }
    }
}