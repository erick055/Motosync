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
            tvSidebarRole.setText(savedRole.substring(0, 1).toUpperCase() + savedRole.substring(1) + " Account");
        }

        if (btnMenu != null) btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // SAFELY ROUTE USING InvoicesActivity.this
        findViewById(R.id.navDashboard).setOnClickListener(v -> { startActivity(new Intent(InvoicesActivity.this, MainActivity.class)); finish(); });
        findViewById(R.id.navBookService).setOnClickListener(v -> { startActivity(new Intent(InvoicesActivity.this, BookingActivity.class)); finish(); });
        findViewById(R.id.navMyVehicles).setOnClickListener(v -> { startActivity(new Intent(InvoicesActivity.this, VehiclesActivity.class)); finish(); });
        findViewById(R.id.navMyOrders).setOnClickListener(v -> { startActivity(new Intent(InvoicesActivity.this, MyOrdersActivity.class)); finish(); });
        findViewById(R.id.navMyInvoices).setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
        findViewById(R.id.navProfile).setOnClickListener(v -> { startActivity(new Intent(InvoicesActivity.this, ProfileActivity.class)); finish(); });

        findViewById(R.id.btnLogoutMenu).setOnClickListener(v -> {
            Toast.makeText(InvoicesActivity.this, "Logging out...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(InvoicesActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
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
        if (lastJobSnapshot == null || lastInvoiceSnapshot == null) return;

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
            TextView noData = new TextView(InvoicesActivity.this);
            noData.setText("You have no completed invoices yet.");
            noData.setTextColor(getResources().getColor(R.color.text_secondary));
            noData.setTextSize(16f);
            invoicesContainer.addView(noData);
        }
    }

    private void addInvoiceCardToScreen(String invoiceId, String service, String amount, String status) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_invoice, invoicesContainer, false);

        ((TextView) cardView.findViewById(R.id.tvInvoiceService)).setText(service);
        ((TextView) cardView.findViewById(R.id.tvInvoiceAmount)).setText("₱ " + amount);

        String shortId = invoiceId != null && invoiceId.length() > 6 ? invoiceId.substring(invoiceId.length() - 6).toUpperCase() : "12345";
        ((TextView) cardView.findViewById(R.id.tvInvoiceId)).setText("Invoice Ref: #" + shortId);

        TextView tvStatus = cardView.findViewById(R.id.tvInvoiceStatus);
        LinearLayout btnPayNow = cardView.findViewById(R.id.btnPayNow);

        tvStatus.setText(status);
        if ("Paid".equalsIgnoreCase(status)) {
            tvStatus.setBackgroundResource(R.drawable.bg_badge_completed);
            btnPayNow.setVisibility(View.GONE);
        } else {
            tvStatus.setBackgroundResource(R.drawable.bg_badge_cancelled);
            btnPayNow.setVisibility(View.VISIBLE);
        }

        btnPayNow.setOnClickListener(v -> {
            // FIXED CONTEXT
            Toast.makeText(InvoicesActivity.this, "Processing Payment...", Toast.LENGTH_SHORT).show();
            mInvoicesRef.child(invoiceId).child("status").setValue("Paid").addOnSuccessListener(aVoid -> {
                Toast.makeText(InvoicesActivity.this, "Payment Successful!", Toast.LENGTH_LONG).show();
            });
        });

        invoicesContainer.addView(cardView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mJobOrdersRef != null && jobOrdersListener != null) mJobOrdersRef.removeEventListener(jobOrdersListener);
        if (mInvoicesRef != null && invoicesListener != null) mInvoicesRef.removeEventListener(invoicesListener);
    }
}