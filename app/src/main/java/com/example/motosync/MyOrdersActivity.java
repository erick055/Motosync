package com.example.motosync;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
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

public class MyOrdersActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private DatabaseReference mJobOrdersRef;
    private LinearLayout ordersContainer;
    private String customerName;

    private ValueEventListener jobOrdersListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_orders);

        mJobOrdersRef = FirebaseDatabase.getInstance().getReference("JobOrders");
        ordersContainer = findViewById(R.id.ordersContainer);
        drawerLayout = findViewById(R.id.drawerLayout);
        ImageView btnMenu = findViewById(R.id.btnMenu);

        SharedPreferences prefs = getSharedPreferences("MotoSyncPrefs", MODE_PRIVATE);
        customerName = prefs.getString("FULL_NAME", "Unknown Customer");
        String savedRole = prefs.getString("ROLE", "customer");

        TextView tvSidebarName = findViewById(R.id.tvSidebarName);
        TextView tvSidebarRole = findViewById(R.id.tvSidebarRole);
        if (tvSidebarName != null) tvSidebarName.setText(customerName);
        if (tvSidebarRole != null && savedRole.length() > 0) {
            tvSidebarRole.setText(savedRole.substring(0, 1).toUpperCase() + savedRole.substring(1) + " Account");
        }

        if (btnMenu != null) btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // =========================================================
        // --- 100% SECURE CUSTOMER SIDEBAR NAVIGATION ---
        // =========================================================

        LinearLayout navDashboard = findViewById(R.id.navDashboard);
        if(navDashboard != null) navDashboard.setOnClickListener(v -> { startActivity(new Intent(MyOrdersActivity.this, MainActivity.class)); finish(); });

        LinearLayout navBookService = findViewById(R.id.navBookService);
        if(navBookService != null) navBookService.setOnClickListener(v -> { startActivity(new Intent(MyOrdersActivity.this, BookingActivity.class)); finish(); });

        LinearLayout navMyVehicles = findViewById(R.id.navMyVehicles);
        if(navMyVehicles != null) navMyVehicles.setOnClickListener(v -> { startActivity(new Intent(MyOrdersActivity.this, VehiclesActivity.class)); finish(); });

        LinearLayout navMyOrders = findViewById(R.id.navMyOrders);
        if(navMyOrders != null) navMyOrders.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START)); // We are already here!

        LinearLayout navMyInvoices = findViewById(R.id.navMyInvoices);
        if(navMyInvoices != null) navMyInvoices.setOnClickListener(v -> { startActivity(new Intent(MyOrdersActivity.this, InvoicesActivity.class)); finish(); });

        LinearLayout navProfile = findViewById(R.id.navProfile);
        if(navProfile != null) navProfile.setOnClickListener(v -> { startActivity(new Intent(MyOrdersActivity.this, ProfileActivity.class)); finish(); });

        LinearLayout btnLogoutMenu = findViewById(R.id.btnLogoutMenu);
        if(btnLogoutMenu != null) btnLogoutMenu.setOnClickListener(v -> {
            Toast.makeText(MyOrdersActivity.this, "Logging out...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MyOrdersActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        fetchMyJobOrders();
    }

    private void fetchMyJobOrders() {
        jobOrdersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ordersContainer.removeAllViews();
                boolean found = false;

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String jobCustomerName = ds.child("customerName").getValue(String.class);

                    if (jobCustomerName != null && jobCustomerName.equalsIgnoreCase(customerName)) {
                        String service = ds.child("serviceType").getValue(String.class);
                        String mechanic = ds.child("assignedMechanic").getValue(String.class);
                        String cost = ds.child("cost").getValue(String.class);
                        String status = ds.child("status").getValue(String.class);

                        if (cost == null) cost = "0.00";
                        if (status == null) status = "Pending";

                        addOrderCardToScreen(service, mechanic, cost, status);
                        found = true;
                    }
                }

                if (!found) {
                    TextView noData = new TextView(MyOrdersActivity.this);
                    noData.setText("You have no active orders.");
                    noData.setTextColor(getResources().getColor(R.color.text_secondary));
                    noData.setTextSize(16f);
                    ordersContainer.addView(noData);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        mJobOrdersRef.addValueEventListener(jobOrdersListener);
    }

    private void addOrderCardToScreen(String service, String mechanic, String cost, String status) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_my_order, ordersContainer, false);

        ((TextView) cardView.findViewById(R.id.tvOrderService)).setText(service);
        ((TextView) cardView.findViewById(R.id.tvOrderMechanic)).setText("Mechanic: " + mechanic);
        ((TextView) cardView.findViewById(R.id.tvOrderCost)).setText("Est. Cost: ₱ " + cost);

        TextView tvStatus = cardView.findViewById(R.id.tvOrderStatus);
        tvStatus.setText(status);

        // Dynamically style the status badge
        switch (status) {
            case "Completed": tvStatus.setBackgroundResource(R.drawable.bg_badge_completed); break;
            case "Cancelled": tvStatus.setBackgroundResource(R.drawable.bg_badge_cancelled); break;
            case "On Hold": tvStatus.setBackgroundResource(R.drawable.bg_badge_purple); break;
            case "Pending": tvStatus.setBackgroundResource(R.drawable.bg_badge_pending); break;
            case "In Progress": tvStatus.setBackgroundResource(R.drawable.bg_badge_primary); break;
            default: tvStatus.setBackgroundResource(R.drawable.bg_badge_green); break;
        }

        ordersContainer.addView(cardView);
    }

    // --- MEMORY LEAK KILL SWITCH ---
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mJobOrdersRef != null && jobOrdersListener != null) {
            mJobOrdersRef.removeEventListener(jobOrdersListener);
        }
    }
}