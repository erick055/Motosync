package com.example.motosync;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import java.util.Calendar;

public class BookingActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private TextView tvDate, tvTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        drawerLayout = findViewById(R.id.drawerLayout);
        ImageView btnMenu = findViewById(R.id.btnMenu);
        Spinner spinnerVehicle = findViewById(R.id.spinnerVehicle);
        Spinner spinnerService = findViewById(R.id.spinnerService);
        LinearLayout btnPickDate = findViewById(R.id.btnPickDate);
        LinearLayout btnPickTime = findViewById(R.id.btnPickTime);
        LinearLayout btnConfirmBooking = findViewById(R.id.btnConfirmBooking);
        tvDate = findViewById(R.id.tvDate);
        tvTime = findViewById(R.id.tvTime);

        // Find Sidebar Menu Items
        LinearLayout navDashboard = findViewById(R.id.navDashboard);
        LinearLayout navBookService = findViewById(R.id.navBookService);
        LinearLayout navMyVehicles = findViewById(R.id.navMyVehicles);
        LinearLayout btnLogoutMenu = findViewById(R.id.btnLogoutMenu);
        LinearLayout navMyOrders = findViewById(R.id.navMyOrders);
        LinearLayout navMyInvoices = findViewById(R.id.navMyInvoices);


        // 1. Setup Menu Drawer
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // --- MENU BUTTON CLICKS ---

        // Go to Dashboard
        navDashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.closeDrawer(GravityCompat.START);
                Intent intent = new Intent(BookingActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); // Close this page
            }
        });

        if (navMyVehicles != null) {
            navMyVehicles.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(this, VehiclesActivity.class));
            });
        }

        if (navMyOrders != null) {
            navMyOrders.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(this, MyOrdersActivity.class));
            });
        }

        if (navMyInvoices != null) {
            navMyInvoices.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(this, InvoicesActivity.class));
            });
        }

        // Already on Booking, just close drawer
        navBookService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.closeDrawer(GravityCompat.START);
            }
        });

        if (btnLogoutMenu != null) {
            btnLogoutMenu.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                Toast.makeText(BookingActivity.this, "Logging out...", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(BookingActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        // (We will connect My Vehicles later once the screen is built!)

        // --------------------------

        // 2. Populate Dropdowns (Spinners)
        String[] vehicles = {"Choose a vehicle...", "Yamaha R1 (Plate: ABC-123)", "Honda Click 125i (Plate: XYZ-987)"};
        ArrayAdapter<String> vehicleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, vehicles);
        spinnerVehicle.setAdapter(vehicleAdapter);

        String[] services = {"Choose a service...", "Change Oil", "Brake Pad Replacement", "Full Engine Overhaul", "Tire Alignment"};
        ArrayAdapter<String> serviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, services);
        spinnerService.setAdapter(serviceAdapter);

        // 3. Setup Date Picker
        btnPickDate.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(BookingActivity.this,
                    (view, year1, monthOfYear, dayOfMonth) -> {
                        tvDate.setText(dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1);
                    }, year, month, day);
            datePickerDialog.show();
        });

        // 4. Setup Time Picker
        btnPickTime.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(BookingActivity.this,
                    (view, hourOfDay, minuteOfHour) -> {
                        String time = String.format("%02d:%02d", hourOfDay, minuteOfHour);
                        tvTime.setText(time);
                    }, hour, minute, true);
            timePickerDialog.show();
        });

        // 5. Submit Button
        btnConfirmBooking.setOnClickListener(v -> {
            Toast.makeText(BookingActivity.this, "Booking Request Sent!", Toast.LENGTH_LONG).show();
            finish();
        });
    }
}