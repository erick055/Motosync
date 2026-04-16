package com.example.motosync;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

public class InvoicesActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invoices);

        drawerLayout = findViewById(R.id.drawerLayout);
        ImageView btnMenu = findViewById(R.id.btnMenu);

        // Download Buttons
        LinearLayout btnDownload1 = findViewById(R.id.btnDownload1);
        LinearLayout btnDownload2 = findViewById(R.id.btnDownload2);

        // Sidebar Navigation IDs
        LinearLayout navDashboard = findViewById(R.id.navDashboard);
        LinearLayout navBookService = findViewById(R.id.navBookService);
        LinearLayout navMyVehicles = findViewById(R.id.navMyVehicles);
        LinearLayout navMyOrders = findViewById(R.id.navMyOrders);
        LinearLayout navMyInvoices = findViewById(R.id.navMyInvoices);

        // Open Menu
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }

        // --- DOWNLOAD BUTTON CLICKS ---
        if (btnDownload1 != null) {
            btnDownload1.setOnClickListener(v -> Toast.makeText(InvoicesActivity.this, "Downloading Invoice #INV-2023-001...", Toast.LENGTH_SHORT).show());
        }

        if (btnDownload2 != null) {
            btnDownload2.setOnClickListener(v -> Toast.makeText(InvoicesActivity.this, "Downloading Invoice #INV-2023-002...", Toast.LENGTH_SHORT).show());
        }

        // --- SIDEBAR CLICKS ---
        if (navDashboard != null) {
            navDashboard.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(InvoicesActivity.this, MainActivity.class));
                finish();
            });
        }

        if (navBookService != null) {
            navBookService.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(InvoicesActivity.this, BookingActivity.class));
                finish();
            });
        }

        if (navMyVehicles != null) {
            navMyVehicles.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(InvoicesActivity.this, VehiclesActivity.class));
                finish();
            });
        }

        if (navMyOrders != null) {
            navMyOrders.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(InvoicesActivity.this, MyOrdersActivity.class));
                finish();
            });
        }

        if (navMyInvoices != null) {
            navMyInvoices.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
        }
    }
}