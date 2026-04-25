package com.example.motosync;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
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

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;

    // Database Listeners
    private DatabaseReference mJobOrdersRef;
    private DatabaseReference mAppointmentsRef;
    private ValueEventListener dashboardListener;
    private ValueEventListener appointmentsListener;

    // UI Containers
    private LinearLayout activeRepairsContainer;
    private LinearLayout serviceVaultContainer;
    private TextView tvUpcomingContent;
    private String savedName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Initialize UI Elements
        drawerLayout = findViewById(R.id.drawerLayout);
        ImageView btnMenu = findViewById(R.id.btnMenu);
        activeRepairsContainer = findViewById(R.id.activeRepairsContainer);
        serviceVaultContainer = findViewById(R.id.serviceVaultContainer);
        tvUpcomingContent = findViewById(R.id.tvUpcomingContent);

        // 2. Fetch User Data
        SharedPreferences prefs = getSharedPreferences("MotoSyncPrefs", MODE_PRIVATE);
        savedName = prefs.getString("FULL_NAME", "Customer");
        String savedRole = prefs.getString("ROLE", "motosync");

        // 3. Update Sidebar and Dashboard Welcome Messages
        TextView tvSidebarName = findViewById(R.id.tvSidebarName);
        TextView tvSidebarRole = findViewById(R.id.tvSidebarRole);
        TextView tvWelcomeMessage = findViewById(R.id.tvWelcomeMessage);

        if (tvSidebarName != null) tvSidebarName.setText(savedName);
        if (tvSidebarRole != null && savedRole.length() > 0) {
            String displayRole = savedRole.substring(0, 1).toUpperCase() + savedRole.substring(1);
            tvSidebarRole.setText(displayRole + " Account");
        }
        if (tvWelcomeMessage != null) {
            String firstName = savedName;
            if (savedName.contains(" ")) firstName = savedName.substring(0, savedName.indexOf(" "));
            tvWelcomeMessage.setText("Welcome back, " + firstName);
        }

        // 4. Open Menu Action
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }

        // =========================================================
        // --- 100% SECURE CUSTOMER SIDEBAR NAVIGATION ---
        // =========================================================

        LinearLayout navDashboard = findViewById(R.id.navDashboard);
        if (navDashboard != null) navDashboard.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));

        LinearLayout navBookService = findViewById(R.id.navBookService);
        if (navBookService != null) navBookService.setOnClickListener(v -> { startActivity(new Intent(MainActivity.this, BookingActivity.class)); finish(); });

        LinearLayout navMyVehicles = findViewById(R.id.navMyVehicles);
        if (navMyVehicles != null) navMyVehicles.setOnClickListener(v -> { startActivity(new Intent(MainActivity.this, VehiclesActivity.class)); finish(); });

        LinearLayout navMyOrders = findViewById(R.id.navMyOrders);
        if (navMyOrders != null) navMyOrders.setOnClickListener(v -> { startActivity(new Intent(MainActivity.this, MyOrdersActivity.class)); finish(); });

        LinearLayout navMyInvoices = findViewById(R.id.navMyInvoices);
        if (navMyInvoices != null) navMyInvoices.setOnClickListener(v -> { startActivity(new Intent(MainActivity.this, InvoicesActivity.class)); finish(); });

        LinearLayout navProfile = findViewById(R.id.navProfile);
        if (navProfile != null) navProfile.setOnClickListener(v -> { startActivity(new Intent(MainActivity.this, ProfileActivity.class)); });

        LinearLayout btnLogoutMenu = findViewById(R.id.btnLogoutMenu);
        // Handle Logout from the Page Button
        if (btnLogoutMenu != null) {
            // FIX: Added setOnClickListener so it waits for you to actually click it!
            btnLogoutMenu.setOnClickListener(v -> {
                Toast.makeText(MainActivity.this, "Logging out...", Toast.LENGTH_SHORT).show();
                AuthUtils.logoutUser(MainActivity.this);
            });
        }
        // =========================================================
        // --- QUICK ACTIONS ON DASHBOARD ---
        // =========================================================
        LinearLayout btnDashboardBookService = findViewById(R.id.btnBookService);
        if (btnDashboardBookService != null) {
            btnDashboardBookService.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, BookingActivity.class)));
        }

        LinearLayout btnDashboardAddVehicle = findViewById(R.id.btnAddVehicle);
        if (btnDashboardAddVehicle != null) {
            btnDashboardAddVehicle.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, VehiclesActivity.class)));
        }

        // =========================================================
        // --- FETCH REAL-TIME DASHBOARD DATA ---
        // =========================================================
        mJobOrdersRef = FirebaseDatabase.getInstance().getReference("JobOrders");
        mAppointmentsRef = FirebaseDatabase.getInstance().getReference("Appointments");

        fetchDashboardData();
    }

    private void fetchDashboardData() {
        if (serviceVaultContainer == null || activeRepairsContainer == null) return;

        // Grab the secure ID we set up in Step 1
        String savedUserId = getSharedPreferences("MotoSyncPrefs", MODE_PRIVATE).getString("USER_ID", "Unknown");

        // 🚀 FIREBASE QUERY: Ask the server for ONLY this user's jobs!
        com.google.firebase.database.Query myJobsQuery = mJobOrdersRef.orderByChild("userId").equalTo(savedUserId);

        dashboardListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                serviceVaultContainer.removeAllViews();
                activeRepairsContainer.removeAllViews();

                boolean foundHistory = false;
                boolean foundActive = false;

                // This snapshot NOW ONLY contains records for this specific user! No filtering needed!
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Boolean isArchived = ds.child("isArchived").getValue(Boolean.class);
                    String service = ds.child("serviceType").getValue(String.class);
                    String mechanic = ds.child("assignedMechanic").getValue(String.class);
                    String cost = ds.child("cost").getValue(String.class);
                    String status = ds.child("status").getValue(String.class);

                    if (isArchived != null && isArchived) {
                        View rowView = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_service_history_row, serviceVaultContainer, false);
                        ((TextView) rowView.findViewById(R.id.tvRowService)).setText(service);
                        ((TextView) rowView.findViewById(R.id.tvRowMechanic)).setText(mechanic != null ? mechanic : "Unassigned");
                        ((TextView) rowView.findViewById(R.id.tvRowCost)).setText("₱ " + (cost != null ? cost : "0.00"));
                        serviceVaultContainer.addView(rowView);
                        foundHistory = true;
                    } else {
                        View cardView = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_my_order, activeRepairsContainer, false);
                        ((TextView) cardView.findViewById(R.id.tvOrderService)).setText(service);
                        ((TextView) cardView.findViewById(R.id.tvOrderMechanic)).setText("Mechanic: " + (mechanic != null ? mechanic : "Unassigned"));
                        ((TextView) cardView.findViewById(R.id.tvOrderCost)).setText("Est. Cost: " + (cost != null ? "₱ " + cost : "TBD"));

                        TextView tvStatus = cardView.findViewById(R.id.tvOrderStatus);
                        if (tvStatus != null) {
                            if (status == null) status = "Pending";
                            tvStatus.setText(status);
                            switch (status) {
                                case "Completed": tvStatus.setBackgroundResource(R.drawable.bg_badge_completed); break;
                                case "Declined": case "Cancelled": tvStatus.setBackgroundResource(R.drawable.bg_badge_cancelled); break;
                                case "On Hold": tvStatus.setBackgroundResource(R.drawable.bg_badge_purple); break;
                                case "Pending": tvStatus.setBackgroundResource(R.drawable.bg_badge_pending); break;
                                case "In Progress": tvStatus.setBackgroundResource(R.drawable.bg_badge_primary); break;
                                default: tvStatus.setBackgroundResource(R.drawable.bg_badge_green); break;
                            }
                        }
                        activeRepairsContainer.addView(cardView);
                        foundActive = true;
                    }
                }

                if (!foundHistory) {
                    TextView noData = new TextView(MainActivity.this);
                    noData.setText("No Service History Found");
                    noData.setTextColor(getResources().getColor(R.color.text_secondary));
                    noData.setTypeface(null, Typeface.ITALIC);
                    noData.setGravity(Gravity.CENTER);
                    noData.setPadding(0, 40, 0, 40);
                    serviceVaultContainer.addView(noData);
                }

                if (!foundActive) {
                    LinearLayout emptyState = new LinearLayout(MainActivity.this);
                    emptyState.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int)(160 * getResources().getDisplayMetrics().density)));
                    emptyState.setBackgroundResource(R.drawable.bg_outlined_card);
                    emptyState.setGravity(Gravity.CENTER);
                    emptyState.setOrientation(LinearLayout.VERTICAL);

                    ImageView icon = new ImageView(MainActivity.this);
                    icon.setLayoutParams(new LinearLayout.LayoutParams((int)(48 * getResources().getDisplayMetrics().density), (int)(48 * getResources().getDisplayMetrics().density)));
                    icon.setImageResource(android.R.drawable.ic_menu_agenda);
                    icon.setColorFilter(getResources().getColor(R.color.card_stroke));
                    emptyState.addView(icon);

                    TextView text = new TextView(MainActivity.this);
                    text.setText("No active repair orders at the moment.");
                    text.setTextColor(getResources().getColor(R.color.text_secondary));
                    text.setTextSize(14f);
                    text.setPadding(0, (int)(8 * getResources().getDisplayMetrics().density), 0, 0);
                    emptyState.addView(text);

                    activeRepairsContainer.addView(emptyState);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        // 🚀 VERY IMPORTANT: Attach the listener to the QUERY, not the database reference!
        myJobsQuery.addValueEventListener(dashboardListener);

        // =======================================================
        // 🚀 FIREBASE QUERY FOR APPOINTMENTS
        // =======================================================
        com.google.firebase.database.Query myAppointmentsQuery = mAppointmentsRef.orderByChild("userId").equalTo(savedUserId);

        appointmentsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int upcomingCount = 0;
                String nextDate = null;

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String status = ds.child("status").getValue(String.class);
                    if ("Pending".equals(status) || "Approved".equals(status)) {
                        upcomingCount++;
                        if (nextDate == null) {
                            nextDate = ds.child("date").getValue(String.class);
                        }
                    }
                }

                if (tvUpcomingContent != null) {
                    if (upcomingCount > 0) {
                        tvUpcomingContent.setText(upcomingCount + " Upcoming\nNext: " + (nextDate != null ? nextDate : "TBD"));
                        tvUpcomingContent.setTextColor(getResources().getColor(R.color.teal_accent));
                        tvUpcomingContent.setTypeface(null, Typeface.BOLD);
                    } else {
                        tvUpcomingContent.setText("No upcoming\nappointments.");
                        tvUpcomingContent.setTextColor(getResources().getColor(R.color.text_secondary));
                        tvUpcomingContent.setTypeface(null, Typeface.ITALIC);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        myAppointmentsQuery.addValueEventListener(appointmentsListener);
    }

    // --- PREVENT MEMORY LEAKS ---
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mJobOrdersRef != null && dashboardListener != null) {
            mJobOrdersRef.removeEventListener(dashboardListener);
        }
        if (mAppointmentsRef != null && appointmentsListener != null) {
            mAppointmentsRef.removeEventListener(appointmentsListener);
        }
    }

}