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

public class VehiclesActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vehicles);

        drawerLayout = findViewById(R.id.drawerLayout);
        ImageView btnMenu = findViewById(R.id.btnMenu);
        LinearLayout btnAddNewVehicle = findViewById(R.id.btnAddNewVehicle);

        // Vehicle 1 Actions
        ImageView btnEditVehicle1 = findViewById(R.id.btnEditVehicle1);
        ImageView btnDeleteVehicle1 = findViewById(R.id.btnDeleteVehicle1);

        // 1. Setup Menu Drawer
        if (btnMenu != null) {
            btnMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }

        // 2. Add New Vehicle Click
        if (btnAddNewVehicle != null) {
            btnAddNewVehicle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(VehiclesActivity.this, "Opening Add Vehicle Form...", Toast.LENGTH_SHORT).show();
                    // Later: Intent intent = new Intent(VehiclesActivity.this, AddVehicleActivity.class);
                    // startActivity(intent);
                }
            });
        }

        // 3. Edit/Delete Clicks (Example for Vehicle 1)
        if (btnEditVehicle1 != null) {
            btnEditVehicle1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(VehiclesActivity.this, "Editing Yamaha R1...", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnDeleteVehicle1 != null) {
            btnDeleteVehicle1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(VehiclesActivity.this, "Deleting Yamaha R1...", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}