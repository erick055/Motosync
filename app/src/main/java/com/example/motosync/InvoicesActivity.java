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

public class InvoicesActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private DatabaseReference mInvoicesRef;
    private DatabaseReference mJobOrdersRef;
    private LinearLayout invoicesContainer;
    private String customerEmail;

    private DataSnapshot lastJobSnapshot;
    private DataSnapshot lastInvoiceSnapshot;

    private ValueEventListener jobOrdersListener;
    private ValueEventListener invoicesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invoices);

        mInvoicesRef = FirebaseDatabase.getInstance().getReference("Invoices");
        mJobOrdersRef = FirebaseDatabase.getInstance().getReference("JobOrders");

        invoicesContainer = findViewById(R.id.invoicesContainer);
        drawerLayout = findViewById(R.id.drawerLayout);
        ImageView btnMenu = findViewById(R.id.btnMenu);

        SharedPreferences prefs = getSharedPreferences("MotoSyncPrefs", MODE_PRIVATE);
        String customerName = prefs.getString("FULL_NAME", "Unknown Customer");
        customerEmail = prefs.getString("EMAIL", "Unknown Email");
        String savedRole = prefs.getString("ROLE", "customer");

        TextView tvSidebarName = findViewById(R.id.tvSidebarName);
        TextView tvSidebarRole = findViewById(R.id.tvSidebarRole);
        if (tvSidebarName != null) tvSidebarName.setText(customerName);
        if (tvSidebarRole != null && savedRole.length() > 0) {
            String displayRole = savedRole.substring(0, 1).toUpperCase() + savedRole.substring(1);
            tvSidebarRole.setText(displayRole + " Account");
        }

        if (btnMenu != null) btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // =========================================================
        // --- 100% SECURE CUSTOMER SIDEBAR NAVIGATION ---
        // =========================================================

        LinearLayout navDashboard = findViewById(R.id.navDashboard);
        if(navDashboard != null) navDashboard.setOnClickListener(v -> { startActivity(new Intent(InvoicesActivity.this, MainActivity.class)); finish(); });

        LinearLayout navBookService = findViewById(R.id.navBookService);
        if(navBookService != null) navBookService.setOnClickListener(v -> { startActivity(new Intent(InvoicesActivity.this, BookingActivity.class)); finish(); });

        LinearLayout navMyVehicles = findViewById(R.id.navMyVehicles);
        if(navMyVehicles != null) navMyVehicles.setOnClickListener(v -> { startActivity(new Intent(InvoicesActivity.this, VehiclesActivity.class)); finish(); });

        LinearLayout navMyOrders = findViewById(R.id.navMyOrders);
        if(navMyOrders != null) navMyOrders.setOnClickListener(v -> { startActivity(new Intent(InvoicesActivity.this, MyOrdersActivity.class)); finish(); });

        LinearLayout navMyInvoices = findViewById(R.id.navMyInvoices);
        if(navMyInvoices != null) navMyInvoices.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START)); // Already here!

        LinearLayout navProfile = findViewById(R.id.navProfile);
        if(navProfile != null) navProfile.setOnClickListener(v -> { startActivity(new Intent(InvoicesActivity.this, ProfileActivity.class)); finish(); });

        LinearLayout btnLogoutMenu = findViewById(R.id.btnLogoutMenu);
        if(btnLogoutMenu != null) btnLogoutMenu.setOnClickListener(v -> {
            Toast.makeText(InvoicesActivity.this, "Logging out...", Toast.LENGTH_SHORT).show();
            // Call the shared AuthUtils method
            AuthUtils.logoutUser(InvoicesActivity.this);
        });

        fetchMyInvoices();
    }

    private void fetchMyInvoices() {
        jobOrdersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                lastJobSnapshot = snapshot;
                refreshUI();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        mJobOrdersRef.addValueEventListener(jobOrdersListener);

        invoicesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                lastInvoiceSnapshot = snapshot;
                refreshUI();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        mInvoicesRef.addValueEventListener(invoicesListener);
    }

    private void refreshUI() {
        if (lastJobSnapshot == null || lastInvoiceSnapshot == null || invoicesContainer == null) return;

        invoicesContainer.removeAllViews();
        boolean found = false;

        for (DataSnapshot ds : lastInvoiceSnapshot.getChildren()) {
            String invEmail = ds.child("customerEmail").getValue(String.class);

            if (invEmail != null && customerEmail != null && invEmail.trim().equalsIgnoreCase(customerEmail.trim())) {

                String invoiceId = ds.child("invoiceId").getValue(String.class);
                String jobId = ds.child("jobOrderId").getValue(String.class);

                String jobStatus = "Unknown";
                if (jobId != null) {
                    for (DataSnapshot jobDs : lastJobSnapshot.getChildren()) {
                        String currentJobId = jobDs.child("jobOrderId").getValue(String.class);
                        if (jobId.equals(currentJobId)) {
                            jobStatus = jobDs.child("status").getValue(String.class);
                            break;
                        }
                    }
                }

                if (jobStatus != null && jobStatus.trim().equalsIgnoreCase("Completed")) {
                    String service = ds.child("serviceType").getValue(String.class);
                    String amount = ds.child("amount").getValue(String.class);
                    String status = ds.child("status").getValue(String.class);

                    if (status == null) status = "Unpaid";
                    if (amount == null) amount = "0.00";

                    addInvoiceCardToScreen(invoiceId, service, amount, status);
                    found = true;
                }
            }
        }

        if (!found) {
            TextView noData = new TextView(this);
            noData.setText("You have no completed invoices yet.");
            noData.setTextColor(getResources().getColor(R.color.text_secondary));
            noData.setTextSize(16f);
            noData.setPadding(0, 40, 0, 40);
            noData.setGravity(android.view.Gravity.CENTER);
            invoicesContainer.addView(noData);
        }
    }

    private void addInvoiceCardToScreen(String invoiceId, String service, String amount, String status) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_invoice, invoicesContainer, false);

        TextView tvService = cardView.findViewById(R.id.tvInvoiceService);
        if(tvService != null) tvService.setText(service);

        TextView tvAmount = cardView.findViewById(R.id.tvInvoiceAmount);
        if(tvAmount != null) tvAmount.setText("₱ " + amount);

        TextView tvId = cardView.findViewById(R.id.tvInvoiceId);
        String shortId = invoiceId != null && invoiceId.length() > 6 ? invoiceId.substring(invoiceId.length() - 6).toUpperCase() : "12345";
        if(tvId != null) tvId.setText("Invoice Ref: #" + shortId);

        TextView tvStatus = cardView.findViewById(R.id.tvInvoiceStatus);

        if (tvStatus != null) {
            tvStatus.setText(status);
            // Just update the colors, NO BUTTON LOGIC!
            if ("Paid".equalsIgnoreCase(status)) {
                tvStatus.setBackgroundResource(R.drawable.bg_badge_completed);
            } else {
                tvStatus.setBackgroundResource(R.drawable.bg_badge_cancelled);
            }
        }

        invoicesContainer.addView(cardView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mJobOrdersRef != null && jobOrdersListener != null) {
            mJobOrdersRef.removeEventListener(jobOrdersListener);
        }
        if (mInvoicesRef != null && invoicesListener != null) {
            mInvoicesRef.removeEventListener(invoicesListener);
        }
    }
}