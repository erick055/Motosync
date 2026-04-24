package com.example.motosync;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
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

public class AdminInvoicesActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private DatabaseReference mInvoicesRef;
    private DatabaseReference mJobOrdersRef;
    private LinearLayout adminInvoicesContainer;

    private DataSnapshot lastJobSnapshot;
    private DataSnapshot lastInvoiceSnapshot;

    private ValueEventListener jobOrdersListener;
    private ValueEventListener invoicesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_invoices);

        mInvoicesRef = FirebaseDatabase.getInstance().getReference("Invoices");
        mJobOrdersRef = FirebaseDatabase.getInstance().getReference("JobOrders");
        adminInvoicesContainer = findViewById(R.id.adminInvoicesContainer);
        drawerLayout = findViewById(R.id.drawerLayout);
        ImageView btnMenu = findViewById(R.id.btnMenu);

        SharedPreferences prefs = getSharedPreferences("MotoSyncPrefs", MODE_PRIVATE);
        String savedName = prefs.getString("FULL_NAME", "Admin Name");

        TextView tvSidebarName = findViewById(R.id.tvSidebarName);
        if (tvSidebarName != null) tvSidebarName.setText(savedName);

        if (btnMenu != null) btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // =========================================================
        // --- 100% SECURE FULL SIDEBAR NAVIGATION ---
        // =========================================================

        LinearLayout navAdminDashboard = findViewById(R.id.navAdminDashboard);
        if(navAdminDashboard != null) navAdminDashboard.setOnClickListener(v -> { startActivity(new Intent(AdminInvoicesActivity.this, AdminDashboardActivity.class)); finish(); });

        LinearLayout navManageBookings = findViewById(R.id.navManageBookings);
        if(navManageBookings != null) navManageBookings.setOnClickListener(v -> { startActivity(new Intent(AdminInvoicesActivity.this, AdminAppointmentsActivity.class)); finish(); });

        LinearLayout navJobOrders = findViewById(R.id.navJobOrders);
        if(navJobOrders != null) navJobOrders.setOnClickListener(v -> { startActivity(new Intent(AdminInvoicesActivity.this, AdminJobOrderActivity.class)); finish(); });

        // --- THE MISSING BUTTONS ADDED BACK IN! ---
        LinearLayout navManageServices = findViewById(R.id.navManageServices);
        if(navManageServices != null) navManageServices.setOnClickListener(v -> { startActivity(new Intent(AdminInvoicesActivity.this, AdminInventoryActivity.class)); finish(); });

        LinearLayout navManageCustomers = findViewById(R.id.navManageCustomers);
        if(navManageCustomers != null) navManageCustomers.setOnClickListener(v -> { startActivity(new Intent(AdminInvoicesActivity.this, AdminCustomersActivity.class)); finish(); });

        LinearLayout navManageReports = findViewById(R.id.navManageReports);
        if(navManageReports != null) navManageReports.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START)); // We are already here!

        LinearLayout btnLogoutMenu = findViewById(R.id.btnLogoutMenu);
        if(btnLogoutMenu != null) btnLogoutMenu.setOnClickListener(v -> {
            Toast.makeText(AdminInvoicesActivity.this, "Logging out...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(AdminInvoicesActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });


        fetchAllInvoices();
    }

    private void fetchAllInvoices() {
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

        adminInvoicesContainer.removeAllViews();
        boolean found = false;

        for (DataSnapshot invDs : lastInvoiceSnapshot.getChildren()) {
            String invoiceId = invDs.child("invoiceId").getValue(String.class);
            String jobId = invDs.child("jobOrderId").getValue(String.class);

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
                String custName = invDs.child("customerName").getValue(String.class);
                String service = invDs.child("serviceType").getValue(String.class);
                String amount = invDs.child("amount").getValue(String.class);
                String status = invDs.child("status").getValue(String.class);

                if (status == null) status = "Unpaid";
                if (amount == null) amount = "0.00";

                addAdminInvoiceCard(invoiceId, jobId, custName, service, amount, status);
                found = true;
            }
        }

        if (!found) {
            TextView noData = new TextView(AdminInvoicesActivity.this);
            noData.setText("No invoices ready. (Jobs must be marked 'Completed' first)");
            noData.setTextColor(getResources().getColor(R.color.text_secondary));
            noData.setTextSize(16f);
            adminInvoicesContainer.addView(noData);
        }
    }

    private void addAdminInvoiceCard(final String invoiceId, final String jobOrderId, String custName, String service, String amount, String status) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_admin_invoice, adminInvoicesContainer, false);

        ((TextView) cardView.findViewById(R.id.tvAdminInvService)).setText(service);
        ((TextView) cardView.findViewById(R.id.tvAdminInvCustomer)).setText("Customer: " + custName);
        ((TextView) cardView.findViewById(R.id.tvAdminInvAmount)).setText("₱ " + amount);

        String shortId = invoiceId != null && invoiceId.length() > 6 ? invoiceId.substring(invoiceId.length() - 6).toUpperCase() : "12345";
        ((TextView) cardView.findViewById(R.id.tvAdminInvId)).setText("Invoice Ref: #" + shortId);

        TextView tvStatus = cardView.findViewById(R.id.tvAdminInvStatus);
        LinearLayout btnMarkPaid = cardView.findViewById(R.id.btnAdminMarkPaid);
        LinearLayout btnEditCost = cardView.findViewById(R.id.btnAdminEditCost);
        LinearLayout btnDelete = cardView.findViewById(R.id.btnAdminDeleteInv);

        tvStatus.setText(status);
        if ("Paid".equalsIgnoreCase(status)) {
            tvStatus.setBackgroundResource(R.drawable.bg_badge_completed);
            btnMarkPaid.setVisibility(View.GONE);
        } else {
            tvStatus.setBackgroundResource(R.drawable.bg_badge_cancelled);
            btnMarkPaid.setVisibility(View.VISIBLE);
        }

        btnEditCost.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(AdminInvoicesActivity.this);
            builder.setTitle("Update Final Cost (₱)");

            final EditText input = new EditText(AdminInvoicesActivity.this);
            input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            input.setText(amount);
            int padding = (int) (20 * getResources().getDisplayMetrics().density);
            input.setPadding(padding, padding, padding, padding);
            builder.setView(input);

            builder.setPositiveButton("Save", (dialog, which) -> {
                String newCost = input.getText().toString().trim();
                if (!newCost.isEmpty()) {
                    mInvoicesRef.child(invoiceId).child("amount").setValue(newCost);
                    if (jobOrderId != null) {
                        mJobOrdersRef.child(jobOrderId).child("cost").setValue(newCost);
                    }
                    Toast.makeText(AdminInvoicesActivity.this, "Cost updated! Customer will see this instantly.", Toast.LENGTH_SHORT).show();
                }
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            builder.show();
        });

        btnMarkPaid.setOnClickListener(v -> {
            mInvoicesRef.child(invoiceId).child("status").setValue("Paid");
            Toast.makeText(AdminInvoicesActivity.this, "Marked as Paid!", Toast.LENGTH_SHORT).show();
        });

        btnDelete.setOnClickListener(v -> {
            mInvoicesRef.child(invoiceId).removeValue();
            Toast.makeText(AdminInvoicesActivity.this, "Invoice Deleted", Toast.LENGTH_SHORT).show();
        });

        adminInvoicesContainer.addView(cardView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mJobOrdersRef != null && jobOrdersListener != null) mJobOrdersRef.removeEventListener(jobOrdersListener);
        if (mInvoicesRef != null && invoicesListener != null) mInvoicesRef.removeEventListener(invoicesListener);
    }
}