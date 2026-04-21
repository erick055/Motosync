package com.example.motosync;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
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

import java.util.Calendar;
import java.util.Locale;

public class BookingActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        mDatabase = FirebaseDatabase.getInstance().getReference("Appointments");

        // UI Setup
        drawerLayout = findViewById(R.id.drawerLayout);
        ImageView btnMenu = findViewById(R.id.btnMenu);

        Spinner spinnerService = findViewById(R.id.spinnerService);
        Spinner spinnerVehicle = findViewById(R.id.spinnerVehicle);

        // Date and Time Elements
        LinearLayout btnPickDate = findViewById(R.id.btnPickDate);
        LinearLayout btnPickTime = findViewById(R.id.btnPickTime);
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

        // --- FIX 1: PROPER SPINNER SETUP ---
        String[] services = {"Change Oil", "Tune Up", "Brake Pad Replacement", "General Checkup"};
        // Use simple_spinner_item for the collapsed view, and dropdown_item for the expanded list
        ArrayAdapter<String> serviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, services);
        serviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerService.setAdapter(serviceAdapter);

        String[] vehicles = {"Yamaha R1 (ABC-123)", "Honda Click 125i (XYZ-987)", "Kawasaki NMAX (DEF-456)"};
        ArrayAdapter<String> vehicleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, vehicles);
        vehicleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVehicle.setAdapter(vehicleAdapter);

        // --- FIX 2: DATE PICKER LOGIC ---
        if (btnPickDate != null) {
            btnPickDate.setOnClickListener(v -> {
                Calendar calendar = Calendar.getInstance();
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int day = calendar.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog datePickerDialog = new DatePickerDialog(BookingActivity.this,
                        (view, selectedYear, selectedMonth, selectedDay) -> {
                            // Months are indexed from 0, so add 1
                            String formattedDate = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                            tvDate.setText(formattedDate);
                        }, year, month, day);

                // Optional: Prevent users from booking in the past
                datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
                datePickerDialog.show();
            });
        }

        // --- FIX 3: TIME PICKER LOGIC ---
        if (btnPickTime != null) {
            btnPickTime.setOnClickListener(v -> {
                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minute = calendar.get(Calendar.MINUTE);

                TimePickerDialog timePickerDialog = new TimePickerDialog(BookingActivity.this,
                        (view, selectedHour, selectedMinute) -> {
                            // Format to ensure it shows 09:05 instead of 9:5
                            String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
                            tvTime.setText(formattedTime);
                        }, hour, minute, false); // 'false' for 12-hour AM/PM format, 'true' for 24-hour
                timePickerDialog.show();
            });
        }

        // --- SUBMIT BOOKING TO FIREBASE ---
        if (btnConfirmBooking != null) {
            btnConfirmBooking.setOnClickListener(v -> {
                String dateStr = tvDate.getText().toString().trim();
                String timeStr = tvTime.getText().toString().trim();
                String selectedService = spinnerService.getSelectedItem().toString();
                String selectedVehicle = spinnerVehicle.getSelectedItem().toString();

                if (dateStr.equals("dd/mm/yyyy")) {
                    Toast.makeText(BookingActivity.this, "Please select a date.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (timeStr.equals("00:00")) {
                    Toast.makeText(BookingActivity.this, "Please select a time.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String fullDateAndTime = dateStr + " at " + timeStr;

                Toast.makeText(BookingActivity.this, "Sending Request...", Toast.LENGTH_SHORT).show();

                String appointmentId = mDatabase.push().getKey();

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

        // --- SIDEBAR NAVIGATION ---
        if (btnMenu != null) btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        LinearLayout navDashboard = findViewById(R.id.navDashboard);
        LinearLayout navBookService = findViewById(R.id.navBookService);
        LinearLayout navMyVehicles = findViewById(R.id.navMyVehicles);
        LinearLayout navMyOrders = findViewById(R.id.navMyOrders);
        LinearLayout navMyInvoices = findViewById(R.id.navMyInvoices);
        LinearLayout btnLogoutMenu = findViewById(R.id.btnLogoutMenu);

        if (navDashboard != null) navDashboard.setOnClickListener(v -> { startActivity(new Intent(this, MainActivity.class)); finish(); });
        if (navBookService != null) navBookService.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
        if (navMyVehicles != null) navMyVehicles.setOnClickListener(v -> { startActivity(new Intent(this, VehiclesActivity.class)); finish(); });
        if (navMyOrders != null) navMyOrders.setOnClickListener(v -> { startActivity(new Intent(this, MyOrdersActivity.class)); finish(); });
        if (navMyInvoices != null) navMyInvoices.setOnClickListener(v -> { startActivity(new Intent(this, InvoicesActivity.class)); finish(); });

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