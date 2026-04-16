package com.example.motosync;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
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

        // Sidebar Navigation
        LinearLayout navDashboard = findViewById(R.id.navDashboard);
        LinearLayout navBookService = findViewById(R.id.navBookService);
        LinearLayout navMyVehicles = findViewById(R.id.navMyVehicles);

        // 1. Setup Menu Drawer
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }

        // --- SIDEBAR CLICKS ---
        if (navDashboard != null) {
            navDashboard.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                Intent intent = new Intent(VehiclesActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            });
        }

        if (navBookService != null) {
            navBookService.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                Intent intent = new Intent(VehiclesActivity.this, BookingActivity.class);
                startActivity(intent);
                finish();
            });
        }

        if (navMyVehicles != null) {
            navMyVehicles.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
        }

        // 2. Add New Vehicle Click -> SHOW POPUP
        if (btnAddNewVehicle != null) {
            btnAddNewVehicle.setOnClickListener(v -> showAddVehicleDialog());
        }
    }

    // --- POPUP DIALOG LOGIC ---
    private void showAddVehicleDialog() {
        // Create the Dialog
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_vehicle);

        // Make the background transparent so your rounded corners show correctly
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        // Ensure the dialog is wide enough
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        // Find the views inside the popup
        EditText etBrand = dialog.findViewById(R.id.etBrand);
        EditText etModel = dialog.findViewById(R.id.etModel);
        EditText etPlateNumber = dialog.findViewById(R.id.etPlateNumber);
        LinearLayout btnSaveVehicle = dialog.findViewById(R.id.btnSaveVehicle);
        TextView btnCancelVehicle = dialog.findViewById(R.id.btnCancelVehicle);

        // Save Button Click inside Popup
        btnSaveVehicle.setOnClickListener(v -> {
            String brand = etBrand.getText().toString().trim();
            String model = etModel.getText().toString().trim();
            String plate = etPlateNumber.getText().toString().trim();

            if (brand.isEmpty() || model.isEmpty() || plate.isEmpty()) {
                Toast.makeText(VehiclesActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // TODO: In the future, send this to PHP backend
            Toast.makeText(VehiclesActivity.this, "Vehicle Saved!", Toast.LENGTH_SHORT).show();
            dialog.dismiss(); // Close the popup
        });

        // Cancel Button Click inside Popup
        btnCancelVehicle.setOnClickListener(v -> dialog.dismiss());

        // Display the Dialog to the user
        dialog.show();
    }
}