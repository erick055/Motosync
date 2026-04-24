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

    // --- NEW: We need to listen to BOTH databases! ---
    private DatabaseReference mAppointmentsRef;
    private DatabaseReference mJobOrdersRef;

    private LinearLayout ordersContainer;
    private String customerName;

    // Data Snapshots to hold our real-time data
    private DataSnapshot lastAppointmentsSnapshot;
    private DataSnapshot lastJobOrdersSnapshot;

    private ValueEventListener appointmentsListener;
    private ValueEventListener jobOrdersListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_orders);

        // Connect to Firebase
        mAppointmentsRef = FirebaseDatabase.getInstance().getReference("Appointments");
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
            // Call the shared AuthUtils method
            AuthUtils.logoutUser(MyOrdersActivity.this);
        });

        // Start listening to the cloud!
        fetchMyOrdersRealTime();
    }

    private void fetchMyOrdersRealTime() {
        // 1. Listen to Appointments (Brand new bookings)
        appointmentsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                lastAppointmentsSnapshot = snapshot;
                refreshUI();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        mAppointmentsRef.addValueEventListener(appointmentsListener);

        // 2. Listen to Job Orders (Admin approved bookings)
        jobOrdersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                lastJobOrdersSnapshot = snapshot;
                refreshUI();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        mJobOrdersRef.addValueEventListener(jobOrdersListener);
    }

    private void refreshUI() {
        // Wait until BOTH databases have responded before drawing the screen
        if (lastAppointmentsSnapshot == null || lastJobOrdersSnapshot == null || ordersContainer == null) return;

        ordersContainer.removeAllViews();
        boolean found = false;

        // Loop through all Appointments to find this customer's requests
        for (DataSnapshot apptDs : lastAppointmentsSnapshot.getChildren()) {
            String apptCustomerName = apptDs.child("customerName").getValue(String.class);

            if (apptCustomerName != null && apptCustomerName.equalsIgnoreCase(customerName)) {

                String apptId = apptDs.child("appointmentId").getValue(String.class);
                String service = apptDs.child("serviceType").getValue(String.class);
                String status = apptDs.child("status").getValue(String.class);

                // Default values if Admin hasn't touched it yet
                String displayMechanic = "Unassigned";
                String displayCost = "TBD";
                if (status == null) status = "Pending";

                // SMART CHECK: Does this Appointment have a connected Job Order yet?
                for (DataSnapshot jobDs : lastJobOrdersSnapshot.getChildren()) {
                    String jobApptId = jobDs.child("appointmentId").getValue(String.class);

                    if (jobApptId != null && jobApptId.equals(apptId)) {
                        // Yes! The Admin approved it. Override the defaults with the real Job Data.
                        String mech = jobDs.child("assignedMechanic").getValue(String.class);
                        String cost = jobDs.child("cost").getValue(String.class);
                        String jobStatus = jobDs.child("status").getValue(String.class);

                        if (mech != null) displayMechanic = mech;
                        if (cost != null) displayCost = "₱ " + cost; // Add peso sign to real numbers
                        if (jobStatus != null) status = jobStatus;
                        break;
                    }
                }

                // Draw the card to the screen
                addOrderCardToScreen(service, displayMechanic, displayCost, status);
                found = true;
            }
        }

        // If the customer has literally booked nothing yet
        if (!found) {
            TextView noData = new TextView(MyOrdersActivity.this);
            noData.setText("You have no active orders or appointments.");
            noData.setTextColor(getResources().getColor(R.color.text_secondary));
            noData.setTextSize(16f);
            ordersContainer.addView(noData);
        }
    }

    private void addOrderCardToScreen(String service, String mechanic, String cost, String status) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_my_order, ordersContainer, false);

        ((TextView) cardView.findViewById(R.id.tvOrderService)).setText(service);
        ((TextView) cardView.findViewById(R.id.tvOrderMechanic)).setText("Mechanic: " + mechanic);
        ((TextView) cardView.findViewById(R.id.tvOrderCost)).setText("Est. Cost: " + cost);

        TextView tvStatus = cardView.findViewById(R.id.tvOrderStatus);
        if (tvStatus != null) {
            tvStatus.setText(status);

            // Dynamically style the status badge
            switch (status) {
                case "Completed": tvStatus.setBackgroundResource(R.drawable.bg_badge_completed); break;
                case "Declined":
                case "Cancelled": tvStatus.setBackgroundResource(R.drawable.bg_badge_cancelled); break;
                case "On Hold": tvStatus.setBackgroundResource(R.drawable.bg_badge_purple); break;
                case "Pending": tvStatus.setBackgroundResource(R.drawable.bg_badge_pending); break;
                case "In Progress": tvStatus.setBackgroundResource(R.drawable.bg_badge_primary); break;
                default: tvStatus.setBackgroundResource(R.drawable.bg_badge_green); break;
            }
        }

        ordersContainer.addView(cardView);
    }

    // --- MEMORY LEAK KILL SWITCH ---
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAppointmentsRef != null && appointmentsListener != null) {
            mAppointmentsRef.removeEventListener(appointmentsListener);
        }
        if (mJobOrdersRef != null && jobOrdersListener != null) {
            mJobOrdersRef.removeEventListener(jobOrdersListener);
        }
    }
}