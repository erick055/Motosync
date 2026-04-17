package com.example.motosync;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

        // Quick Action Buttons
        LinearLayout btnApproveOrder = findViewById(R.id.btnApproveOrder);
        LinearLayout btnDeclineOrder = findViewById(R.id.btnDeclineOrder);

        // Admin Sidebar Navigation IDs
        LinearLayout navAdminDashboard = findViewById(R.id.navAdminDashboard);
        LinearLayout navManageOrders = findViewById(R.id.navManageOrders);
        LinearLayout navManageCustomers = findViewById(R.id.navManageCustomers);
        LinearLayout navManageServices = findViewById(R.id.navManageServices);
        LinearLayout btnLogoutMenu = findViewById(R.id.btnLogoutMenu);

        // Open Menu
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }

        // --- DASHBOARD ACTIONS ---
        if (btnApproveOrder != null) {
            btnApproveOrder.setOnClickListener(v -> Toast.makeText(AdminDashboardActivity.this, "Order Approved & Moved to Active queue", Toast.LENGTH_SHORT).show());
        }

        if (btnDeclineOrder != null) {
            btnDeclineOrder.setOnClickListener(v -> Toast.makeText(AdminDashboardActivity.this, "Order Declined", Toast.LENGTH_SHORT).show());
        }

        // --- ADMIN SIDEBAR CLICKS ---
        if (navAdminDashboard != null) {
            navAdminDashboard.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
        }

        if (navManageOrders != null) {
            navManageOrders.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                Toast.makeText(this, "Opening Manage Orders...", Toast.LENGTH_SHORT).show();
                // startActivity(new Intent(this, AdminOrdersActivity.class));
            });
        }

        if (navManageCustomers != null) {
            navManageCustomers.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                Toast.makeText(this, "Opening Customer Directory...", Toast.LENGTH_SHORT).show();
            });
        }

        if (navManageServices != null) {
            navManageServices.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                Toast.makeText(this, "Opening Service & Price Editor...", Toast.LENGTH_SHORT).show();
            });
        }

        // Handle Logout
        if (btnLogoutMenu != null) {
            btnLogoutMenu.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                Toast.makeText(this, "Admin Logging out...", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }
}