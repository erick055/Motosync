package com.example.motosync;

import android.app.AlertDialog;
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
    private LinearLayout invoicesContainer;
    private DatabaseReference mInvoicesRef;
    private String savedName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invoices);

        drawerLayout = findViewById(R.id.drawerLayout);
        invoicesContainer = findViewById(R.id.invoicesContainer);
        mInvoicesRef = FirebaseDatabase.getInstance().getReference("Invoices");

        // Fetch User Identity
        SharedPreferences prefs = getSharedPreferences("MotoSyncPrefs", MODE_PRIVATE);
        savedName = prefs.getString("FULL_NAME", "Customer");

        setupSidebar();
        fetchMyInvoices();
    }

    private void fetchMyInvoices() {
        mInvoicesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (invoicesContainer == null) return;
                invoicesContainer.removeAllViews();
                boolean hasInvoices = false;

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String custName = ds.child("customerName").getValue(String.class);

                    // Filter only invoices belonging to the logged-in customer!
                    if (custName != null && custName.equals(savedName)) {
                        String invoiceId = ds.child("invoiceId").getValue(String.class);
                        String service = ds.child("serviceType").getValue(String.class);
                        String amount = ds.child("amount").getValue(String.class);
                        String status = ds.child("status").getValue(String.class);

                        if (status == null) status = "Unpaid";
                        if (amount == null) amount = "0.00";

                        addInvoiceCard(invoiceId, service, amount, status);
                        hasInvoices = true;
                    }
                }

                if (!hasInvoices) {
                    TextView noData = new TextView(InvoicesActivity.this);
                    noData.setText("You have no invoices at this time.");
                    noData.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));
                    invoicesContainer.addView(noData);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(InvoicesActivity.this, "Failed to load invoices.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addInvoiceCard(String invoiceId, String service, String amount, String status) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_invoice, invoicesContainer, false);

        TextView tvService = cardView.findViewById(R.id.tvInvService);
        TextView tvAmount = cardView.findViewById(R.id.tvInvAmount);
        TextView tvRef = cardView.findViewById(R.id.tvInvRef);
        TextView tvStatus = cardView.findViewById(R.id.tvInvStatus);

        if (tvService != null) tvService.setText(service != null ? service : "Unknown Service");
        if (tvAmount != null) tvAmount.setText("Total: ₱ " + amount);

        // Generate a clean looking short reference ID
        if (tvRef != null) {
            String shortId = invoiceId != null && invoiceId.length() > 6 ? invoiceId.substring(invoiceId.length() - 6).toUpperCase() : "REF";
            tvRef.setText("Ref: #" + shortId);
        }

        if (tvStatus != null) {
            tvStatus.setText(status);

            // Just update the colors, no buttons to hide/show anymore!
            if (status.equals("Paid")) {
                tvStatus.setBackgroundResource(R.drawable.bg_badge_completed); // Green Badge
            } else {
                tvStatus.setBackgroundResource(R.drawable.bg_badge_cancelled); // Red Badge
            }
        }

        invoicesContainer.addView(cardView);
    }

    private void setupSidebar() {
        ImageView btnMenu = findViewById(R.id.btnMenu);
        if (btnMenu != null) btnMenu.setOnClickListener(v -> {
            if (drawerLayout != null) drawerLayout.openDrawer(GravityCompat.START);
        });

        TextView tvSidebarName = findViewById(R.id.tvSidebarName);
        if (tvSidebarName != null) tvSidebarName.setText(savedName);

        // Sidebar Links
        View navDashboard = findViewById(R.id.navDashboard);
        if (navDashboard != null) navDashboard.setOnClickListener(v -> { startActivity(new Intent(this, MainActivity.class)); finish(); });

        View navBookService = findViewById(R.id.navBookService);
        if (navBookService != null) navBookService.setOnClickListener(v -> { startActivity(new Intent(this, BookingActivity.class)); finish(); });

        View navMyVehicles = findViewById(R.id.navMyVehicles);
        if (navMyVehicles != null) navMyVehicles.setOnClickListener(v -> { startActivity(new Intent(this, VehiclesActivity.class)); finish(); });

        View navMyOrders = findViewById(R.id.navMyOrders);
        if (navMyOrders != null) navMyOrders.setOnClickListener(v -> { startActivity(new Intent(this, MyOrdersActivity.class)); finish(); });

        View navMyInvoices = findViewById(R.id.navMyInvoices);
        if (navMyInvoices != null) navMyInvoices.setOnClickListener(v -> { if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START); });

        View btnLogoutMenu = findViewById(R.id.btnLogoutMenu);
        if (btnLogoutMenu != null) btnLogoutMenu.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}