package com.example.motosync;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class BookingActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        // Initialize Firebase pointing to the "Appointments" folder
        mDatabase = FirebaseDatabase.getInstance().getReference("Appointments");

        // UI Setup
        drawerLayout = findViewById(R.id.drawerLayout);
        ImageView btnMenu = findViewById(R.id.btnMenu);

        // THESE NOW MATCH YOUR XML PERFECTLY
        Spinner spinnerService = findViewById(R.id.spinnerService);
        Spinner spinnerVehicle = findViewById(R.id.spinnerVehicle);
        TextView tvDate = findViewById(R.id.tvDate);
        TextView tvTime = findViewById(R.id.tvTime);
        LinearLayout btnConfirmBooking = findViewById(R.id.btnConfirmBooking);

        // --- FETCH LOGGED-IN CUSTOMER DETAILS ---
        SharedPreferences prefs = getSharedPreferences("MotoSyncPrefs", MODE_PRIVATE);
        String customerName = prefs.getString("FULL_NAME", "Unknown Customer");
        String customerEmail = prefs.getString("EMAIL", "Unknown Email");
        String savedRole = prefs.getString("ROLE", "customer");

        // Sync Sidebar Name
        TextView tvSidebarName = findViewById(R.id.tvSidebarName);
        TextView tvSidebarRole = findViewById(R.id.tvSidebarRole);
        if (tvSidebarName != null) tvSidebarName.setText(customerName);
        if (tvSidebarRole != null && savedRole.length() > 0) {
            String displayRole = savedRole.substring(0, 1).toUpperCase() + savedRole.substring(1);
            tvSidebarRole.setText(displayRole + " Account");
        }
        // ----------------------------------------

        // Setup Dummy Data for Spinners
        String[] services = {"Change Oil", "Tune Up", "Brake Pad Replacement", "General Checkup"};
        ArrayAdapter<String> serviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, services);
        spinnerService.setAdapter(serviceAdapter);

        String[] vehicles = {"Yamaha R1 (ABC-123)", "Honda Click 125i (XYZ-987)", "Kawasaki NMAX (DEF-456)"};
        ArrayAdapter<String> vehicleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, vehicles);
        spinnerVehicle.setAdapter(vehicleAdapter);

        // --- SUBMIT BOOKING TO FIREBASE ---
        if (btnConfirmBooking != null) {
            btnConfirmBooking.setOnClickListener(v -> {
                String dateStr = tvDate.getText().toString().trim();
                String timeStr = tvTime.getText().toString().trim();
                String selectedService = spinnerService.getSelectedItem().toString();
                String selectedVehicle = spinnerVehicle.getSelectedItem().toString();

                // Combine date and time for Firebase
                String fullDateAndTime = dateStr + " at " + timeStr;

                if (dateStr.equals("dd/mm/yyyy") || dateStr.isEmpty()) {
                    Toast.makeText(BookingActivity.this, "Please select a date.", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(BookingActivity.this, "Sending Request...", Toast.LENGTH_SHORT).show();

                String appointmentId = mDatabase.push().getKey();

                // Create the Appointment object (No notes field, matching your UI)
                Appointment newAppointment = new Appointment(
                        appointmentId,
                        customerName,
                        customerEmail,
                        selectedService,
                        selectedVehicle,
                        fullDateAndTime,
                        "Pending"
                );

                if (appointmentId != null) {
                    mDatabase.child(appointmentId).setValue(newAppointment)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(BookingActivity.this, "Booking Confirmed!", Toast.LENGTH_LONG).show();
                                startActivity(new Intent(BookingActivity.this, MyOrdersActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e -> Toast.makeText(BookingActivity.this, "Failed to book: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            });
        }

        // Open Menu
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }

        // --- SIDEBAR NAVIGATION ---
        LinearLayout navDashboard = findViewById(R.id.navDashboard);
        LinearLayout navBookService = findViewById(R.id.navBookService);
        LinearLayout navMyVehicles = findViewById(R.id.navMyVehicles);
        LinearLayout navMyOrders = findViewById(R.id.navMyOrders);
        LinearLayout navMyInvoices = findViewById(R.id.navMyInvoices);
        LinearLayout btnLogoutMenu = findViewById(R.id.btnLogoutMenu);

        if (navDashboard != null) {
            navDashboard.setOnClickListener(v -> {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            });
        }

        if (navBookService != null) {
            navBookService.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
        }

        if (navMyVehicles != null) {
            navMyVehicles.setOnClickListener(v -> {
                startActivity(new Intent(this, VehiclesActivity.class));
                finish();
            });
        }

        if (navMyOrders != null) {
            navMyOrders.setOnClickListener(v -> {
                startActivity(new Intent(this, MyOrdersActivity.class));
                finish();
            });
        }

        if (navMyInvoices != null) {
            navMyInvoices.setOnClickListener(v -> {
                startActivity(new Intent(this, InvoicesActivity.class));
                finish();
            });
        }

        if (btnLogoutMenu != null) {
            btnLogoutMenu.setOnClickListener(v -> {
                Toast.makeText(BookingActivity.this, "Logging out...", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(BookingActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }
}