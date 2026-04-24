package com.example.motosync;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.util.HashMap;

public class VehiclesActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private DatabaseReference mVehiclesRef;
    private LinearLayout vehiclesContainer;
    private String savedName;
    private ValueEventListener vehiclesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vehicles);

        drawerLayout = findViewById(R.id.drawerLayout);
        ImageView btnMenu = findViewById(R.id.btnMenu);
        LinearLayout btnAddNewVehicle = findViewById(R.id.btnAddNewVehicle);
        vehiclesContainer = findViewById(R.id.vehiclesContainer);

        mVehiclesRef = FirebaseDatabase.getInstance().getReference("Vehicles");

        // --- FETCH AND DISPLAY USER DATA ---
        SharedPreferences prefs = getSharedPreferences("MotoSyncPrefs", MODE_PRIVATE);
        savedName = prefs.getString("FULL_NAME", "Customer");
        String savedRole = prefs.getString("ROLE", "motosync");

        TextView tvSidebarName = findViewById(R.id.tvSidebarName);
        TextView tvSidebarRole = findViewById(R.id.tvSidebarRole);
        if (tvSidebarName != null) tvSidebarName.setText(savedName);
        if (tvSidebarRole != null && savedRole.length() > 0) {
            tvSidebarRole.setText(savedRole.substring(0, 1).toUpperCase() + savedRole.substring(1) + " Account");
        }

        if (btnMenu != null) btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // SIDEBAR NAVIGATION
        LinearLayout navDashboard = findViewById(R.id.navDashboard);
        if (navDashboard != null) navDashboard.setOnClickListener(v -> { startActivity(new Intent(this, MainActivity.class)); finish(); });

        LinearLayout navBookService = findViewById(R.id.navBookService);
        if (navBookService != null) navBookService.setOnClickListener(v -> { startActivity(new Intent(this, BookingActivity.class)); finish(); });

        LinearLayout navMyVehicles = findViewById(R.id.navMyVehicles);
        if (navMyVehicles != null) navMyVehicles.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START)); // Already here!

        LinearLayout navMyOrders = findViewById(R.id.navMyOrders);
        if (navMyOrders != null) navMyOrders.setOnClickListener(v -> { startActivity(new Intent(this, MyOrdersActivity.class)); finish(); });

        LinearLayout navMyInvoices = findViewById(R.id.navMyInvoices);
        if (navMyInvoices != null) navMyInvoices.setOnClickListener(v -> { startActivity(new Intent(this, InvoicesActivity.class)); finish(); });

        LinearLayout navProfile = findViewById(R.id.navProfile);
        if (navProfile != null) navProfile.setOnClickListener(v -> { startActivity(new Intent(this, ProfileActivity.class)); finish(); });

        LinearLayout btnLogoutMenu = findViewById(R.id.btnLogoutMenu);
        if(btnLogoutMenu != null) btnLogoutMenu.setOnClickListener(v -> {
            Toast.makeText(VehiclesActivity.this, "Logging out...", Toast.LENGTH_SHORT).show();
            // Call the shared AuthUtils method
            AuthUtils.logoutUser(VehiclesActivity.this);
        });

        if (btnAddNewVehicle != null) {
            btnAddNewVehicle.setOnClickListener(v -> showAddVehicleDialog());
        }

        // Load Vehicles from Firebase
        fetchMyVehicles();
    }

    private void fetchMyVehicles() {
        vehiclesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                vehiclesContainer.removeAllViews();
                boolean hasVehicles = false;

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String customerName = ds.child("customerName").getValue(String.class);

                    // Check if this vehicle belongs to the logged-in user
                    if (customerName != null && customerName.equalsIgnoreCase(savedName)) {
                        String vehicleId = ds.child("vehicleId").getValue(String.class);
                        String brand = ds.child("brand").getValue(String.class);
                        String model = ds.child("model").getValue(String.class);
                        String plate = ds.child("plate").getValue(String.class);

                        View cardView = LayoutInflater.from(VehiclesActivity.this).inflate(R.layout.item_my_vehicle, vehiclesContainer, false);

                        ((TextView) cardView.findViewById(R.id.tvVehicleName)).setText(brand + " " + model);
                        ((TextView) cardView.findViewById(R.id.tvVehiclePlate)).setText("Plate: " + plate);

                        cardView.findViewById(R.id.btnDeleteVehicle).setOnClickListener(v -> {
                            if (vehicleId != null) mVehiclesRef.child(vehicleId).removeValue();
                            Toast.makeText(VehiclesActivity.this, "Vehicle Deleted", Toast.LENGTH_SHORT).show();
                        });

                        vehiclesContainer.addView(cardView);
                        hasVehicles = true;
                    }
                }

                if (!hasVehicles) {
                    TextView noData = new TextView(VehiclesActivity.this);
                    noData.setText("You haven't added any vehicles yet.");
                    noData.setTextColor(getResources().getColor(R.color.text_secondary));
                    noData.setPadding(0, 20, 0, 20);
                    vehiclesContainer.addView(noData);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        mVehiclesRef.addValueEventListener(vehiclesListener);
    }

    private void showAddVehicleDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_vehicle);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        EditText etBrand = dialog.findViewById(R.id.etBrand);
        EditText etModel = dialog.findViewById(R.id.etModel);
        EditText etPlateNumber = dialog.findViewById(R.id.etPlateNumber);
        LinearLayout btnSaveVehicle = dialog.findViewById(R.id.btnSaveVehicle);
        TextView btnCancelVehicle = dialog.findViewById(R.id.btnCancelVehicle);

        btnSaveVehicle.setOnClickListener(v -> {
            String brand = etBrand.getText().toString().trim();
            String model = etModel.getText().toString().trim();
            String plate = etPlateNumber.getText().toString().trim();

            if (brand.isEmpty() || model.isEmpty() || plate.isEmpty()) {
                Toast.makeText(VehiclesActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // --- PUSH TO FIREBASE ---
            String vehicleId = mVehiclesRef.push().getKey();
            HashMap<String, Object> vehicleData = new HashMap<>();
            vehicleData.put("vehicleId", vehicleId);
            vehicleData.put("customerName", savedName);
            vehicleData.put("brand", brand);
            vehicleData.put("model", model);
            vehicleData.put("plate", plate);

            if (vehicleId != null) mVehiclesRef.child(vehicleId).setValue(vehicleData);

            Toast.makeText(VehiclesActivity.this, "Vehicle Saved to Garage!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        btnCancelVehicle.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mVehiclesRef != null && vehiclesListener != null) {
            mVehiclesRef.removeEventListener(vehiclesListener);
        }
    }
}