package com.example.motosync;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class BookingActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private DatabaseReference mDatabase;
    private DatabaseReference mVehiclesRef;
    private ValueEventListener vehiclesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        mDatabase = FirebaseDatabase.getInstance().getReference("Appointments");
        mVehiclesRef = FirebaseDatabase.getInstance().getReference("Vehicles");

        // UI Setup
        drawerLayout = findViewById(R.id.drawerLayout);
        ImageView btnMenu = findViewById(R.id.btnMenu);

        // NEW: Problem Description Box instead of Spinner
        EditText etProblemDescription = findViewById(R.id.etProblemDescription);

        Spinner spinnerVehicle = findViewById(R.id.spinnerVehicle);
        LinearLayout btnPickDate = findViewById(R.id.btnPickDate);
        LinearLayout btnPickTime = findViewById(R.id.btnPickTime);
        TextView tvDate = findViewById(R.id.tvDate);
        TextView tvTime = findViewById(R.id.tvTime);
        LinearLayout btnConfirmBooking = findViewById(R.id.btnConfirmBooking);

        SharedPreferences prefs = getSharedPreferences("MotoSyncPrefs", MODE_PRIVATE);
        String customerName = prefs.getString("FULL_NAME", "Unknown Customer");
        String customerEmail = prefs.getString("EMAIL", "Unknown Email");
        String savedRole = prefs.getString("ROLE", "customer");

        TextView tvSidebarName = findViewById(R.id.tvSidebarName);
        TextView tvSidebarRole = findViewById(R.id.tvSidebarRole);
        if (tvSidebarName != null) tvSidebarName.setText(customerName);
        if (tvSidebarRole != null && savedRole.length() > 0) {
            String displayRole = savedRole.substring(0, 1).toUpperCase() + savedRole.substring(1);
            tvSidebarRole.setText(displayRole + " Account");
        }

        // --- DYNAMIC FIREBASE VEHICLE SPINNER SETUP ---
        List<String> vehicleList = new ArrayList<>();
        vehicleList.add("Loading vehicles...");

        ArrayAdapter<String> vehicleAdapter = new ArrayAdapter<>(this, R.layout.item_spinner, vehicleList);
        vehicleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVehicle.setAdapter(vehicleAdapter);

        // Fetch exactly the logged-in customer's vehicles
        vehiclesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                vehicleList.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String owner = ds.child("customerName").getValue(String.class);
                    if (owner != null && owner.equalsIgnoreCase(customerName)) {
                        String brand = ds.child("brand").getValue(String.class);
                        String model = ds.child("model").getValue(String.class);
                        String plate = ds.child("plate").getValue(String.class);
                        vehicleList.add(brand + " " + model + " (" + plate + ")");
                    }
                }

                if (vehicleList.isEmpty()) {
                    vehicleList.add("No Vehicles Found (Please add one)");
                }
                vehicleAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        mVehiclesRef.addValueEventListener(vehiclesListener);

        // --- DATE PICKER ---
        if (btnPickDate != null) {
            btnPickDate.setOnClickListener(v -> {
                Calendar calendar = Calendar.getInstance();
                DatePickerDialog datePickerDialog = new DatePickerDialog(BookingActivity.this,
                        (view, selectedYear, selectedMonth, selectedDay) -> {
                            String formattedDate = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                            tvDate.setText(formattedDate);
                        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
                datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
                datePickerDialog.show();
            });
        }

        // --- TIME PICKER ---
        if (btnPickTime != null) {
            btnPickTime.setOnClickListener(v -> {
                Calendar calendar = Calendar.getInstance();
                TimePickerDialog timePickerDialog = new TimePickerDialog(BookingActivity.this,
                        (view, selectedHour, selectedMinute) -> {
                            String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
                            tvTime.setText(formattedTime);
                        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false);
                timePickerDialog.show();
            });
        }


        // --- SUBMIT BOOKING ---
        if (btnConfirmBooking != null) {
            btnConfirmBooking.setOnClickListener(v -> {
                String selectedService = etProblemDescription.getText().toString().trim();
                String dateStr = tvDate.getText().toString().trim();
                String timeStr = tvTime.getText().toString().trim();
                String selectedVehicle = spinnerVehicle.getSelectedItem().toString();

                if (selectedService.isEmpty() || dateStr.equals("Select Date") || timeStr.equals("Select Time")) {
                    Toast.makeText(BookingActivity.this, "Please fill out all fields.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (selectedVehicle.contains("Loading") || selectedVehicle.contains("No Vehicles")) {
                    Toast.makeText(BookingActivity.this, "Please add a vehicle first.", Toast.LENGTH_LONG).show();
                    return;
                }

                // GRAB THE SECURE USER ID!
                String savedUserId = getSharedPreferences("MotoSyncPrefs", MODE_PRIVATE).getString("USER_ID", "Unknown");

                String fullDateAndTime = dateStr + " at " + timeStr;
                Toast.makeText(BookingActivity.this, "Sending Request...", Toast.LENGTH_SHORT).show();

                String appointmentId = mDatabase.push().getKey();

                // Add savedUserId to the Appointment constructor!
                Appointment newAppointment = new Appointment(
                        appointmentId, customerName, customerEmail, savedUserId, selectedService, selectedVehicle, fullDateAndTime, "Pending"
                );

                if (appointmentId != null) {
                    mDatabase.child(appointmentId).setValue(newAppointment)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(BookingActivity.this, "Booking Confirmed!", Toast.LENGTH_LONG).show();
                                startActivity(new Intent(BookingActivity.this, MyOrdersActivity.class));
                                finish();
                            });
                }
            });
        }

        // --- 100% SECURE SIDEBAR NAVIGATION ---
        if (btnMenu != null) btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        LinearLayout navDashboard = findViewById(R.id.navDashboard);
        LinearLayout navBookService = findViewById(R.id.navBookService);
        LinearLayout navMyVehicles = findViewById(R.id.navMyVehicles);
        LinearLayout navMyOrders = findViewById(R.id.navMyOrders);
        LinearLayout navMyInvoices = findViewById(R.id.navMyInvoices);
        LinearLayout navProfile = findViewById(R.id.navProfile);
        LinearLayout btnLogoutMenu = findViewById(R.id.btnLogoutMenu);

        if (navDashboard != null) navDashboard.setOnClickListener(v -> { startActivity(new Intent(this, MainActivity.class)); finish(); });
        if (navBookService != null) navBookService.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
        if (navMyVehicles != null) navMyVehicles.setOnClickListener(v -> { startActivity(new Intent(this, VehiclesActivity.class)); finish(); });
        if (navMyOrders != null) navMyOrders.setOnClickListener(v -> { startActivity(new Intent(this, MyOrdersActivity.class)); finish(); });
        if (navMyInvoices != null) navMyInvoices.setOnClickListener(v -> { startActivity(new Intent(this, InvoicesActivity.class)); finish(); });
        if (navProfile != null) navProfile.setOnClickListener(v -> { startActivity(new Intent(this, ProfileActivity.class)); finish(); });


        if(btnLogoutMenu != null) btnLogoutMenu.setOnClickListener(v -> {
            Toast.makeText(BookingActivity.this, "Logging out...", Toast.LENGTH_SHORT).show();
            // Call the shared AuthUtils method
            AuthUtils.logoutUser(BookingActivity.this);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mVehiclesRef != null && vehiclesListener != null) {
            mVehiclesRef.removeEventListener(vehiclesListener);
        }
    }
}