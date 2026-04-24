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

    // Variables for the History Book Feature
    private DatabaseReference mJobOrdersRef;
    private ValueEventListener historyListener;
    private LinearLayout serviceVaultContainer;
    private String savedName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Initialize UI Elements
        drawerLayout = findViewById(R.id.drawerLayout);
        ImageView btnMenu = findViewById(R.id.btnMenu);
        serviceVaultContainer = findViewById(R.id.serviceVaultContainer); // The dynamic history container

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
        if (navDashboard != null) navDashboard.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START)); // We are already here!

        LinearLayout navBookService = findViewById(R.id.navBookService);
        if (navBookService != null) navBookService.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, BookingActivity.class));
            finish();
        });

        LinearLayout navMyVehicles = findViewById(R.id.navMyVehicles);
        if (navMyVehicles != null) navMyVehicles.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, VehiclesActivity.class));
            finish();
        });

        LinearLayout navMyOrders = findViewById(R.id.navMyOrders);
        if (navMyOrders != null) navMyOrders.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, MyOrdersActivity.class));
            finish();
        });

        LinearLayout navMyInvoices = findViewById(R.id.navMyInvoices);
        if (navMyInvoices != null) navMyInvoices.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, InvoicesActivity.class));
            finish();
        });

        LinearLayout navProfile = findViewById(R.id.navProfile);
        if (navProfile != null) navProfile.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
            finish();
        });

        LinearLayout btnLogoutMenu = findViewById(R.id.btnLogoutMenu);
        if (btnLogoutMenu != null) {
            btnLogoutMenu.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                Toast.makeText(MainActivity.this, "Logging out...", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        // =========================================================
        // --- FETCH REAL-TIME HISTORY DATA ---
        // =========================================================
        mJobOrdersRef = FirebaseDatabase.getInstance().getReference("JobOrders");
        fetchServiceHistory();
    }

    // Method to dynamically pull Archived Job Orders into the Service Vault
    private void fetchServiceHistory() {
        if (serviceVaultContainer == null) return;

        historyListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                serviceVaultContainer.removeAllViews();
                boolean foundHistory = false;

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String jobCustomerName = ds.child("customerName").getValue(String.class);
                    Boolean isArchived = ds.child("isArchived").getValue(Boolean.class);

                    // Check if data belongs to this user AND has been pushed to history
                    if (jobCustomerName != null && jobCustomerName.equalsIgnoreCase(savedName) && isArchived != null && isArchived) {

                        String service = ds.child("serviceType").getValue(String.class);
                        String mechanic = ds.child("assignedMechanic").getValue(String.class);
                        String cost = ds.child("cost").getValue(String.class);

                        // Draw the minimal history row using the XML we created
                        View rowView = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_service_history_row, serviceVaultContainer, false);

                        TextView tvRowService = rowView.findViewById(R.id.tvRowService);
                        TextView tvRowMechanic = rowView.findViewById(R.id.tvRowMechanic);
                        TextView tvRowCost = rowView.findViewById(R.id.tvRowCost);

                        if (tvRowService != null) tvRowService.setText(service);
                        if (tvRowMechanic != null) tvRowMechanic.setText(mechanic != null ? mechanic : "Unassigned");
                        if (tvRowCost != null) tvRowCost.setText("₱ " + (cost != null ? cost : "0.00"));

                        serviceVaultContainer.addView(rowView);
                        foundHistory = true;
                    }
                }

                // Show default text if they have no history yet
                if (!foundHistory) {
                    TextView noData = new TextView(MainActivity.this);
                    noData.setText("No Service History Found");
                    noData.setTextColor(getResources().getColor(R.color.text_secondary));
                    noData.setTypeface(null, Typeface.ITALIC);
                    noData.setGravity(Gravity.CENTER);
                    noData.setPadding(0, 40, 0, 40);
                    serviceVaultContainer.addView(noData);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        mJobOrdersRef.addValueEventListener(historyListener);
    }

    // --- MEMORY LEAK KILL SWITCH ---
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mJobOrdersRef != null && historyListener != null) {
            mJobOrdersRef.removeEventListener(historyListener);
        }
    }
}