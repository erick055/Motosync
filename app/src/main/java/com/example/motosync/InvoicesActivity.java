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
    private LinearLayout invoicesContainer;
    private String customerEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invoices);

        mInvoicesRef = FirebaseDatabase.getInstance().getReference("Invoices");
        invoicesContainer = findViewById(R.id.invoicesContainer);
        drawerLayout = findViewById(R.id.drawerLayout);
        ImageView btnMenu = findViewById(R.id.btnMenu);

        // --- FETCH LOGGED-IN CUSTOMER DETAILS ---
        SharedPreferences prefs = getSharedPreferences("MotoSyncPrefs", MODE_PRIVATE);
        String customerName = prefs.getString("FULL_NAME", "Unknown Customer");
        customerEmail = prefs.getString("EMAIL", "Unknown Email"); // Used for filtering!
        String savedRole = prefs.getString("ROLE", "customer");

        // Sync Sidebar Name
        TextView tvSidebarName = findViewById(R.id.tvSidebarName);
        TextView tvSidebarRole = findViewById(R.id.tvSidebarRole);
        if (tvSidebarName != null) tvSidebarName.setText(customerName);
        if (tvSidebarRole != null && savedRole.length() > 0) {
            String displayRole = savedRole.substring(0, 1).toUpperCase() + savedRole.substring(1);
            tvSidebarRole.setText(displayRole + " Account");
        }

        // Open Menu
        if (btnMenu != null) btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // --- SIDEBAR NAVIGATION ---
        findViewById(R.id.navDashboard).setOnClickListener(v -> { startActivity(new Intent(this, MainActivity.class)); finish(); });
        findViewById(R.id.navBookService).setOnClickListener(v -> { startActivity(new Intent(this, BookingActivity.class)); finish(); });
        findViewById(R.id.navMyVehicles).setOnClickListener(v -> { startActivity(new Intent(this, VehiclesActivity.class)); finish(); });
        findViewById(R.id.navMyOrders).setOnClickListener(v -> { startActivity(new Intent(this, MyOrdersActivity.class)); finish(); });
        findViewById(R.id.navMyInvoices).setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
        findViewById(R.id.navProfile).setOnClickListener(v -> { startActivity(new Intent(this, ProfileActivity.class)); finish(); });
        findViewById(R.id.btnLogoutMenu).setOnClickListener(v -> {
            Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Fetch Invoices specific to this user
        fetchMyInvoices();
    }

    private void fetchMyInvoices() {
        // Query Firebase to only pull invoices matching the logged-in email
        mInvoicesRef.orderByChild("customerEmail").equalTo(customerEmail).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                invoicesContainer.removeAllViews();

                if (!snapshot.exists()) {
                    TextView noData = new TextView(InvoicesActivity.this);
                    noData.setText("You have no invoices yet.");
                    noData.setTextColor(getResources().getColor(R.color.text_secondary));
                    noData.setTextSize(16f);
                    invoicesContainer.addView(noData);
                    return;
                }

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String invoiceId = ds.child("invoiceId").getValue(String.class);
                    String service = ds.child("serviceType").getValue(String.class);
                    String amount = ds.child("amount").getValue(String.class);
                    String status = ds.child("status").getValue(String.class);

                    if (status == null) status = "Unpaid";
                    if (amount == null) amount = "0.00";

                    addInvoiceCardToScreen(invoiceId, service, amount, status);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(InvoicesActivity.this, "Failed to load invoices.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addInvoiceCardToScreen(String invoiceId, String service, String amount, String status) {
        // Ensure you have the 'item_invoice.xml' file we created earlier!
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_invoice, invoicesContainer, false);

        ((TextView) cardView.findViewById(R.id.tvInvoiceService)).setText(service);
        ((TextView) cardView.findViewById(R.id.tvInvoiceAmount)).setText("₱ " + amount);

        // Format a short ID for display
        String shortId = invoiceId != null && invoiceId.length() > 6 ? invoiceId.substring(invoiceId.length() - 6).toUpperCase() : "12345";
        ((TextView) cardView.findViewById(R.id.tvInvoiceId)).setText("Invoice Ref: #" + shortId);

        // Status Badge Logic
        TextView tvStatus = cardView.findViewById(R.id.tvInvoiceStatus);
        LinearLayout btnPayNow = cardView.findViewById(R.id.btnPayNow);

        tvStatus.setText(status);
        if (status.equals("Paid")) {
            tvStatus.setBackgroundResource(R.drawable.bg_badge_completed); // Green
            btnPayNow.setVisibility(View.GONE); // Hide button if already paid
        } else {
            tvStatus.setBackgroundResource(R.drawable.bg_badge_cancelled); // Red/Orange
            btnPayNow.setVisibility(View.VISIBLE);
        }

        // --- SIMULATE PAYMENT ---
        btnPayNow.setOnClickListener(v -> {
            Toast.makeText(this, "Processing Payment...", Toast.LENGTH_SHORT).show();

            // Update Firebase status to "Paid"
            mInvoicesRef.child(invoiceId).child("status").setValue("Paid").addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Payment Successful!", Toast.LENGTH_LONG).show();
            });
        });

        invoicesContainer.addView(cardView);
    }
}