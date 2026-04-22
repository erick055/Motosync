package com.example.motosync;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CalendarView;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AdminAppointmentsActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private DatabaseReference mDatabase;
    private LinearLayout appointmentsContainer;
    private TextView tvSelectedDateLabel;
    private List<Appointment> allAppointments = new ArrayList<>(); // Caches all data

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_appointments);

        drawerLayout = findViewById(R.id.drawerLayout);
        mDatabase = FirebaseDatabase.getInstance().getReference("Appointments");
        appointmentsContainer = findViewById(R.id.appointmentsContainer);
        tvSelectedDateLabel = findViewById(R.id.tvSelectedDateLabel);
        CalendarView calendarView = findViewById(R.id.calendarView);
        ImageView btnMenu = findViewById(R.id.btnMenu);

        if (btnMenu != null) btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Setup Sidebar Routing (Dashboard, Calendar, Job Orders)
        findViewById(R.id.navAdminDashboard).setOnClickListener(v -> { startActivity(new Intent(this, AdminDashboardActivity.class)); finish(); });
        findViewById(R.id.navJobOrders).setOnClickListener(v -> { startActivity(new Intent(this, AdminJobOrderActivity.class)); finish(); });

        // Get Today's Date
        Calendar c = Calendar.getInstance();
        String todayString = c.get(Calendar.DAY_OF_MONTH) + "/" + (c.get(Calendar.MONTH) + 1) + "/" + c.get(Calendar.YEAR);
        tvSelectedDateLabel.setText("Appointments for: " + todayString);

        // Fetch Data Once
        fetchAppointments(todayString);

        // Filter when user taps a new date
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            String selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
            tvSelectedDateLabel.setText("Appointments for: " + selectedDate);
            displayAppointmentsForDate(selectedDate);
        });
    }

    private void fetchAppointments(String initialDate) {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allAppointments.clear();
                for (DataSnapshot apptSnapshot : snapshot.getChildren()) {
                    Appointment appt = apptSnapshot.getValue(Appointment.class);
                    if (appt != null) allAppointments.add(appt);
                }
                displayAppointmentsForDate(initialDate);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void displayAppointmentsForDate(String dateToMatch) {
        appointmentsContainer.removeAllViews();
        boolean found = false;

        for (Appointment appt : allAppointments) {
            // FIX: Added null checks (appt.date != null) to prevent crashes
            if (appt != null && appt.date != null && appt.date.startsWith(dateToMatch)) {
                addAppointmentCardToScreen(appt);
                found = true;
            }
        }

        if (!found) {
            TextView noData = new TextView(this);
            noData.setText("No appointments scheduled for this date.");
            // FIX: Updated color resource calling method to the modern standard
            noData.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));
            appointmentsContainer.addView(noData);
        }
    }

    private void addAppointmentCardToScreen(Appointment appt) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_admin_appointment, appointmentsContainer, false);

        // FIX: Added fallback text ("Unknown") just in case any other Firebase field is missing
        ((TextView) cardView.findViewById(R.id.tvApptService)).setText(appt.serviceType != null ? appt.serviceType : "Unknown Service");
        ((TextView) cardView.findViewById(R.id.tvApptCustomer)).setText("Customer: " + (appt.customerName != null ? appt.customerName : "Unknown"));
        ((TextView) cardView.findViewById(R.id.tvApptVehicle)).setText("Vehicle: " + (appt.vehicleDetails != null ? appt.vehicleDetails : "Unknown"));
        ((TextView) cardView.findViewById(R.id.tvApptDate)).setText("Time: " + (appt.date != null ? appt.date : "Unknown"));
        ((TextView) cardView.findViewById(R.id.tvApptStatus)).setText(appt.status != null ? appt.status : "Pending");

        // Make it VIEW ONLY by completely hiding the action buttons!
        cardView.findViewById(R.id.layoutActionButtons).setVisibility(View.GONE);

        TextView btnDelete = cardView.findViewById(R.id.btnDelete);
        btnDelete.setOnClickListener(v -> {
            // Build a confirmation dialog before deleting
            new android.app.AlertDialog.Builder(AdminAppointmentsActivity.this)
                    .setTitle("Delete Appointment")
                    .setMessage("Are you sure you want to permanently delete this appointment?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        // Remove from Firebase using the unique appointmentId
                        if (appt.appointmentId != null) {
                            mDatabase.child(appt.appointmentId).removeValue()
                                    .addOnSuccessListener(aVoid -> Toast.makeText(AdminAppointmentsActivity.this, "Appointment Deleted", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e -> Toast.makeText(AdminAppointmentsActivity.this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        appointmentsContainer.addView(cardView);
    }
}