package com.example.motosync;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Find the Drawer Layout and the Menu Icon
        drawerLayout = findViewById(R.id.drawerLayout);
        ImageView btnMenu = findViewById(R.id.btnMenu);

        // 2. Find existing buttons
        LinearLayout btnBookService = findViewById(R.id.btnBookService);
        LinearLayout btnAddVehicle = findViewById(R.id.btnAddVehicle);

        // 3. Open Sidebar when menu icon is clicked
        btnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // This slides the drawer out from the left (START) side
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        // Existing click listeners
        btnBookService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Opening Booking Screen...", Toast.LENGTH_SHORT).show();
            }
        });

        btnAddVehicle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Opening Add Vehicle Screen...", Toast.LENGTH_SHORT).show();
            }
        });
    }
}