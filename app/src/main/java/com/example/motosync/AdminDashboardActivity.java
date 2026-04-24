package com.example.motosync;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdminDashboardActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;

    // UI Elements for Data
    private TextView tvTotalRevenue;
    private TextView tvActiveJobs;
    private TextView tvPendingAppointments;
    private TextView tvTotalCustomers;

    // Firebase References
    private DatabaseReference mInvoicesRef;
    private DatabaseReference mJobOrdersRef;
    private DatabaseReference mAppointmentsRef;
    private DatabaseReference mUsersRef;

    // Listeners for Memory Leak Prevention
    private ValueEventListener invoicesListener;
    private ValueEventListener jobOrdersListener;
    private ValueEventListener appointmentsListener;
    private ValueEventListener usersListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        drawerLayout = findViewById(R.id.drawerLayout);
        ImageView btnMenu = findViewById(R.id.btnMenu);

        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        tvActiveJobs = findViewById(R.id.tvActiveJobs);
        tvPendingAppointments = findViewById(R.id.tvPendingAppointments);
        tvTotalCustomers = findViewById(R.id.tvTotalCustomers);
        TextView tvWelcomeName = findViewById(R.id.tvWelcomeName);

        // Fetch user info from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("MotoSyncPrefs", MODE_PRIVATE);
        String savedName = prefs.getString("FULL_NAME", "Admin");

        tvWelcomeName.setText("Welcome back, " + savedName + "!");

        // Update Sidebar Data
        TextView tvSidebarName = findViewById(R.id.tvSidebarName);
        if (tvSidebarName != null) tvSidebarName.setText(savedName);

        // Menu Toggle
        if (btnMenu != null) btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // =========================================================
        // --- 100% SECURE SIDEBAR NAVIGATION (CRASH-PROOF) ---
        // =========================================================

        LinearLayout navAdminDashboard = findViewById(R.id.navAdminDashboard);
        if(navAdminDashboard != null) navAdminDashboard.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START)); // We are already here!

        LinearLayout navManageBookings = findViewById(R.id.navManageBookings);
        if(navManageBookings != null) navManageBookings.setOnClickListener(v -> { startActivity(new Intent(AdminDashboardActivity.this, AdminAppointmentsActivity.class)); finish(); });

        LinearLayout navJobOrders = findViewById(R.id.navJobOrders);
        if(navJobOrders != null) navJobOrders.setOnClickListener(v -> { startActivity(new Intent(AdminDashboardActivity.this, AdminJobOrderActivity.class)); finish(); });

        LinearLayout navManageServices = findViewById(R.id.navManageServices);
        if(navManageServices != null) navManageServices.setOnClickListener(v -> { startActivity(new Intent(AdminDashboardActivity.this, AdminInventoryActivity.class)); finish(); });

        LinearLayout navManageCustomers = findViewById(R.id.navManageCustomers);
        if(navManageCustomers != null) navManageCustomers.setOnClickListener(v -> { startActivity(new Intent(AdminDashboardActivity.this, AdminCustomersActivity.class)); finish(); });

        LinearLayout navManageReports = findViewById(R.id.navManageReports);
        if(navManageReports != null) navManageReports.setOnClickListener(v -> { startActivity(new Intent(AdminDashboardActivity.this, AdminInvoicesActivity.class)); finish(); });

        LinearLayout navAdminHistory = findViewById(R.id.navAdminHistory);
        if (navAdminHistory != null) navAdminHistory.setOnClickListener(v -> {
            startActivity(new Intent(v.getContext(), AdminHistoryActivity.class));
            finish();
        });

        LinearLayout btnLogoutMenu = findViewById(R.id.btnLogoutMenu);
        if(btnLogoutMenu != null) btnLogoutMenu.setOnClickListener(v -> {
            Toast.makeText(AdminDashboardActivity.this, "Logging out...", Toast.LENGTH_SHORT).show();
            // Call the shared AuthUtils method
            AuthUtils.logoutUser(AdminDashboardActivity.this);
        });


        // Initialize Firebase Connections
        mInvoicesRef = FirebaseDatabase.getInstance().getReference("Invoices");
        mJobOrdersRef = FirebaseDatabase.getInstance().getReference("JobOrders");
        mAppointmentsRef = FirebaseDatabase.getInstance().getReference("Appointments");
        mUsersRef = FirebaseDatabase.getInstance().getReference("Users");

        // Start fetching Live Data!
        fetchDashboardData();
    }

    private void fetchDashboardData() {

        // 1. Fetch Total Revenue (Only count "Paid" invoices)
        invoicesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double totalRevenue = 0.0;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String status = ds.child("status").getValue(String.class);
                    if ("Paid".equalsIgnoreCase(status)) {
                        String amountStr = ds.child("amount").getValue(String.class);
                        if (amountStr != null) {
                            try {
                                totalRevenue += Double.parseDouble(amountStr);
                            } catch (NumberFormatException e) {
                                // Ignore unparseable amounts so the app doesn't crash
                            }
                        }
                    }
                }
                tvTotalRevenue.setText(String.format("₱ %,.2f", totalRevenue));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        mInvoicesRef.addValueEventListener(invoicesListener);


        // 2. Fetch Active Jobs (Only count "In Progress")
        jobOrdersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int activeCount = 0;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String status = ds.child("status").getValue(String.class);
                    if ("In Progress".equalsIgnoreCase(status)) {
                        activeCount++;
                    }
                }
                tvActiveJobs.setText(String.valueOf(activeCount));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        mJobOrdersRef.addValueEventListener(jobOrdersListener);


        // 3. Fetch Pending Appointments
        appointmentsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int pendingCount = 0;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String status = ds.child("status").getValue(String.class);
                    if ("Pending".equalsIgnoreCase(status)) {
                        pendingCount++;
                    }
                }
                tvPendingAppointments.setText(String.valueOf(pendingCount));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        mAppointmentsRef.addValueEventListener(appointmentsListener);


        // 4. Fetch Total Customers
        usersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int customerCount = 0;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String role = ds.child("role").getValue(String.class);
                    if ("customer".equalsIgnoreCase(role)) {
                        customerCount++;
                    }
                }
                tvTotalCustomers.setText(String.valueOf(customerCount));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        mUsersRef.addValueEventListener(usersListener);
    }

    // --- THE MEMORY LEAK KILL SWITCH ---
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mInvoicesRef != null && invoicesListener != null) mInvoicesRef.removeEventListener(invoicesListener);
        if (mJobOrdersRef != null && jobOrdersListener != null) mJobOrdersRef.removeEventListener(jobOrdersListener);
        if (mAppointmentsRef != null && appointmentsListener != null) mAppointmentsRef.removeEventListener(appointmentsListener);
        if (mUsersRef != null && usersListener != null) mUsersRef.removeEventListener(usersListener);
    }
}