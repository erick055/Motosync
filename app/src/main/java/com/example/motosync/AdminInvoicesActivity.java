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
    private DatabaseReference mJobOrdersRef; // To sync the price backward
    private LinearLayout adminInvoicesContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_invoices);

        mInvoicesRef = FirebaseDatabase.getInstance().getReference("Invoices");
        mJobOrdersRef = FirebaseDatabase.getInstance().getReference("JobOrders");
        adminInvoicesContainer = findViewById(R.id.adminInvoicesContainer);
        drawerLayout = findViewById(R.id.drawerLayout);
        ImageView btnMenu = findViewById(R.id.btnMenu);

        // Sidebar Name Sync
        SharedPreferences prefs = getSharedPreferences("MotoSyncPrefs", MODE_PRIVATE);
        String savedName = prefs.getString("FULL_NAME", "Admin Name");

        TextView tvSidebarName = findViewById(R.id.tvSidebarName);
        if (tvSidebarName != null) tvSidebarName.setText(savedName);

        if (btnMenu != null) btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Sidebar Routing
        findViewById(R.id.navAdminDashboard).setOnClickListener(v -> { startActivity(new Intent(this, AdminDashboardActivity.class)); finish(); });
        findViewById(R.id.navManageBookings).setOnClickListener(v -> { startActivity(new Intent(this, AdminAppointmentsActivity.class)); finish(); });
        findViewById(R.id.navJobOrders).setOnClickListener(v -> { startActivity(new Intent(this, AdminJobOrderActivity.class)); finish(); });
        findViewById(R.id.navManageReports).setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
        findViewById(R.id.btnLogoutMenu).setOnClickListener(v -> {
            Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Fetch All System Invoices
        fetchAllInvoices();
    }

    private void fetchAllInvoices() {
        mInvoicesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                adminInvoicesContainer.removeAllViews();

                if (!snapshot.exists()) {
                    TextView noData = new TextView(AdminInvoicesActivity.this);
                    noData.setText("No financial records found.");
                    noData.setTextColor(getResources().getColor(R.color.text_secondary));
                    adminInvoicesContainer.addView(noData);
                    return;
                }

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String invoiceId = ds.child("invoiceId").getValue(String.class);
                    String jobOrderId = ds.child("jobOrderId").getValue(String.class); // Needed for syncing cost!
                    String custName = ds.child("customerName").getValue(String.class);
                    String service = ds.child("serviceType").getValue(String.class);
                    String amount = ds.child("amount").getValue(String.class);
                    String status = ds.child("status").getValue(String.class);

                    if (status == null) status = "Unpaid";
                    if (amount == null) amount = "0.00";

                    addAdminInvoiceCard(invoiceId, jobOrderId, custName, service, amount, status);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addAdminInvoiceCard(String invoiceId, String jobOrderId, String custName, String service, String amount, String status) {
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
        if (status.equals("Paid")) {
            tvStatus.setBackgroundResource(R.drawable.bg_badge_completed);
            btnMarkPaid.setVisibility(View.GONE); // Hide if already paid
        } else {
            tvStatus.setBackgroundResource(R.drawable.bg_badge_cancelled);
            btnMarkPaid.setVisibility(View.VISIBLE);
        }

        // --- 1. EDIT COST LOGIC ---
        btnEditCost.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Update Final Cost (₱)");

            final EditText input = new EditText(this);
            input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            input.setText(amount); // Pre-fill with current amount
            int padding = (int) (20 * getResources().getDisplayMetrics().density);
            input.setPadding(padding, padding, padding, padding);
            builder.setView(input);

            builder.setPositiveButton("Save", (dialog, which) -> {
                String newCost = input.getText().toString().trim();
                if (!newCost.isEmpty()) {
                    // Update Invoice
                    mInvoicesRef.child(invoiceId).child("amount").setValue(newCost);

                    // Sync backward to update the Job Order too!
                    if (jobOrderId != null) {
                        mJobOrdersRef.child(jobOrderId).child("cost").setValue(newCost);
                    }

                    Toast.makeText(this, "Cost updated! Customer will see this instantly.", Toast.LENGTH_SHORT).show();
                }
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            builder.show();
        });

        // --- 2. MARK AS PAID LOGIC ---
        btnMarkPaid.setOnClickListener(v -> {
            mInvoicesRef.child(invoiceId).child("status").setValue("Paid");
            Toast.makeText(this, "Marked as Paid!", Toast.LENGTH_SHORT).show();
        });

        // --- 3. DELETE LOGIC ---
        btnDelete.setOnClickListener(v -> {
            mInvoicesRef.child(invoiceId).removeValue();
            Toast.makeText(this, "Invoice Deleted", Toast.LENGTH_SHORT).show();
        });

        adminInvoicesContainer.addView(cardView);
    }
}