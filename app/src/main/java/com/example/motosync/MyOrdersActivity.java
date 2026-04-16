package com.example.motosync;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

public class MyOrdersActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_orders);

        drawerLayout = findViewById(R.id.drawerLayout);
        ImageView btnMenu = findViewById(R.id.btnMenu);

        // Sidebar Navigation IDs
        LinearLayout navDashboard = findViewById(R.id.navDashboard);
        LinearLayout navBookService = findViewById(R.id.navBookService);
        LinearLayout navMyVehicles = findViewById(R.id.navMyVehicles);
        LinearLayout navMyOrders = findViewById(R.id.navMyOrders);

        // Open Menu
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }

        // --- SIDEBAR CLICKS ---
        if (navDashboard != null) {
            navDashboard.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(MyOrdersActivity.this, MainActivity.class));
                finish();
            });
        }

        if (navBookService != null) {
            navBookService.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(MyOrdersActivity.this, BookingActivity.class));
                finish();
            });
        }

        if (navMyVehicles != null) {
            navMyVehicles.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(MyOrdersActivity.this, VehiclesActivity.class));
                finish();
            });
        }

        if (navMyOrders != null) {
            navMyOrders.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
        }
    }
}