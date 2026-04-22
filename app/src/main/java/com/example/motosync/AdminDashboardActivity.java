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

public class AdminDashboardActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        drawerLayout = findViewById(R.id.drawerLayout);
        ImageView btnMenu = findViewById(R.id.btnMenu);

        // --- FETCH AND DISPLAY ADMIN DATA ---
        android.content.SharedPreferences prefs = getSharedPreferences("MotoSyncPrefs", MODE_PRIVATE);
        String savedName = prefs.getString("FULL_NAME", "Admin Name");
        String savedRole = prefs.getString("ROLE", "admin");

        TextView tvSidebarName = findViewById(R.id.tvSidebarName);
        TextView tvSidebarRole = findViewById(R.id.tvSidebarRole);

        if (tvSidebarName != null) {
            tvSidebarName.setText(savedName);
        }
        if (tvSidebarRole != null && savedRole.length() > 0) {
            String displayRole = savedRole.substring(0, 1).toUpperCase() + savedRole.substring(1);
            tvSidebarRole.setText(displayRole + " Account");
        }
        // -----------------------------------

        // Quick Action Page Buttons
        LinearLayout btnApproveOrder = findViewById(R.id.btnApproveOrder);
        LinearLayout btnDeclineOrder = findViewById(R.id.btnDeclineOrder);

        // Admin Sidebar Navigation IDs
        LinearLayout navAdminDashboard = findViewById(R.id.navAdminDashboard);
        LinearLayout navManageBookings = findViewById(R.id.navManageBookings);
        LinearLayout navManageCustomers = findViewById(R.id.navManageCustomers);
        LinearLayout navManageServices = findViewById(R.id.navManageServices);
        LinearLayout navManageReports = findViewById(R.id.navManageReports);
        LinearLayout btnLogoutMenu = findViewById(R.id.btnLogoutMenu);
        LinearLayout navJobOrders = findViewById(R.id.navJobOrders);

        // Open Menu
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }

        // --- DASHBOARD ACTIONS ---
        if (btnApproveOrder != null) {
            btnApproveOrder.setOnClickListener(v -> Toast.makeText(AdminDashboardActivity.this, "Order Approved!", Toast.LENGTH_SHORT).show());
        }

        if (btnDeclineOrder != null) {
            btnDeclineOrder.setOnClickListener(v -> Toast.makeText(AdminDashboardActivity.this, "Order Declined", Toast.LENGTH_SHORT).show());
        }

        // --- ADMIN SIDEBAR CLICKS ---
        if (navAdminDashboard != null) {
            navAdminDashboard.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
        }

        if (navManageBookings != null) {
            navManageBookings.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(this, AdminAppointmentsActivity.class));
            });
        }
        if (navJobOrders != null) {
            navJobOrders.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(this, AdminJobOrderActivity.class));
            });
        }

        if (navManageCustomers != null) {
            navManageCustomers.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(this, AdminCustomersActivity.class));
            });
        }

        if (navManageServices != null) {
            navManageServices.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(this, AdminInventoryActivity.class));
            });
        }

        if (navManageReports != null) {
            navManageReports.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                Toast.makeText(this, "Opening Financial Reports...", Toast.LENGTH_SHORT).show();
            });
        }

        // Handle Logout
        if (btnLogoutMenu != null) {
            btnLogoutMenu.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }
}