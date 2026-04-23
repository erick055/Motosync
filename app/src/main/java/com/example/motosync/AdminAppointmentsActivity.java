package com.example.motosync;

import android.content.Intent;
import android.content.SharedPreferences;
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
    private List<Appointment> allAppointments = new ArrayList<>();

    // --- MEMORY LEAK PREVENTION VARIABLE ---
    private ValueEventListener appointmentsListener;

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

        SharedPreferences prefs = getSharedPreferences("MotoSyncPrefs", MODE_PRIVATE);
        String savedName = prefs.getString("FULL_NAME", "Admin Name");
        String savedRole = prefs.getString("ROLE", "admin");

        TextView tvSidebarName = findViewById(R.id.tvSidebarName);
        TextView tvSidebarRole = findViewById(R.id.tvSidebarRole);
        if (tvSidebarName != null) tvSidebarName.setText(savedName);
        if (tvSidebarRole != null && savedRole.length() > 0) {
            String displayRole = savedRole.substring(0, 1).toUpperCase() + savedRole.substring(1);
            tvSidebarRole.setText(displayRole + " Account");
        }

        if (btnMenu != null)
            btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        findViewById(R.id.navAdminDashboard).setOnClickListener(v -> {
            startActivity(new Intent(this, AdminDashboardActivity.class));
            finish();
        });
        findViewById(R.id.navManageBookings).setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
        findViewById(R.id.navJobOrders).setOnClickListener(v -> {
            startActivity(new Intent(this, AdminJobOrderActivity.class));
            finish();
        });
        findViewById(R.id.navManageReports).setOnClickListener(v -> {
            startActivity(new Intent(this, AdminInvoicesActivity.class));
            finish();
        });
        findViewById(R.id.btnLogoutMenu).setOnClickListener(v -> {
            Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        Calendar c = Calendar.getInstance();
        String todayString = c.get(Calendar.DAY_OF_MONTH) + "/" + (c.get(Calendar.MONTH) + 1) + "/" + c.get(Calendar.YEAR);
        tvSelectedDateLabel.setText("Appointments for: " + todayString);

        fetchAppointments(todayString);

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            String selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
            tvSelectedDateLabel.setText("Appointments for: " + selectedDate);
            displayAppointmentsForDate(selectedDate);
        });
    }

    private void fetchAppointments(String initialDate) {
        appointmentsListener = new ValueEventListener() {
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
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        mDatabase.addValueEventListener(appointmentsListener);
    }

    private void displayAppointmentsForDate(String dateToMatch) {
        appointmentsContainer.removeAllViews();
        boolean found = false;

        for (Appointment appt : allAppointments) {
            if (appt.date.startsWith(dateToMatch)) {
                addAppointmentCardToScreen(appt);
                found = true;
            }
        }

        if (!found) {
            TextView noData = new TextView(this);
            noData.setText("No appointments scheduled for this date.");
            noData.setTextColor(getResources().getColor(R.color.text_secondary));
            appointmentsContainer.addView(noData);
        }
    }

    private void addAppointmentCardToScreen(Appointment appt) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_admin_appointment, appointmentsContainer, false);

        ((TextView) cardView.findViewById(R.id.tvApptService)).setText(appt.serviceType);
        ((TextView) cardView.findViewById(R.id.tvApptCustomer)).setText("Customer: " + appt.customerName);
        ((TextView) cardView.findViewById(R.id.tvApptVehicle)).setText("Vehicle: " + appt.vehicleDetails);
        ((TextView) cardView.findViewById(R.id.tvApptDate)).setText("Time: " + appt.date);

        TextView tvStatus = cardView.findViewById(R.id.tvApptStatus);
        tvStatus.setText(appt.status);

        switch (appt.status) {
            case "Pending":
                tvStatus.setBackgroundResource(R.drawable.bg_badge_pending);
                break;
            case "Approved":
                tvStatus.setBackgroundResource(R.drawable.bg_badge_green);
                break;
            case "Declined":
                tvStatus.setBackgroundResource(R.drawable.bg_badge_cancelled);
                break;
            case "In Progress":
                tvStatus.setBackgroundResource(R.drawable.bg_badge_primary);
                break;
            case "Completed":
                tvStatus.setBackgroundResource(R.drawable.bg_badge_completed);
                break;
            default:
                tvStatus.setBackgroundResource(R.drawable.bg_badge_purple);
                break;
        }

        LinearLayout layoutActionButtons = cardView.findViewById(R.id.layoutActionButtons);
        TextView btnApprove = cardView.findViewById(R.id.btnApprove);
        TextView btnDecline = cardView.findViewById(R.id.btnDecline);
        LinearLayout btnDeleteAppt = cardView.findViewById(R.id.btnDeleteAppt);

        // 1. New Request State
        if ("Pending".equals(appt.status)) {
            layoutActionButtons.setVisibility(View.VISIBLE);
            btnDeleteAppt.setVisibility(View.GONE);

            btnApprove.setOnClickListener(v -> mDatabase.child(appt.appointmentId).child("status").setValue("Approved"));
            btnDecline.setOnClickListener(v -> mDatabase.child(appt.appointmentId).child("status").setValue("Declined"));

        } else {
            // Hide Approve/Decline buttons if already decided
            layoutActionButtons.setVisibility(View.GONE);

            // 2. Cleanup State: Only allow deleting finished/dead appointments!
            if ("Completed".equals(appt.status) || "Declined".equals(appt.status) || "Cancelled".equals(appt.status)) {
                btnDeleteAppt.setVisibility(View.VISIBLE);

                // THE CASCADING DELETE
                btnDeleteAppt.setOnClickListener(v -> {
                    new android.app.AlertDialog.Builder(this)
                            .setTitle("Clear Record?")
                            .setMessage("This will permanently delete this appointment, its Job Order, and the final Invoice from the database. Continue?")
                            .setPositiveButton("Delete All", (dialog, which) -> {

                                // A) Delete the Original Appointment
                                mDatabase.child(appt.appointmentId).removeValue();

                                // B) Cascade Delete (Hunt down and destroy the Job Order and Invoice)
                                DatabaseReference jobRef = FirebaseDatabase.getInstance().getReference("JobOrders");
                                DatabaseReference invRef = FirebaseDatabase.getInstance().getReference("Invoices");

                                jobRef.orderByChild("appointmentId").equalTo(appt.appointmentId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        for (DataSnapshot ds : snapshot.getChildren()) {
                                            String jobId = ds.child("jobOrderId").getValue(String.class);
                                            String invId = ds.child("invoiceId").getValue(String.class);

                                            // Kill the children!
                                            if (jobId != null) jobRef.child(jobId).removeValue();
                                            if (invId != null) invRef.child(invId).removeValue();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                    }
                                });

                                Toast.makeText(this, "All linked records deleted!", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                });
            } else {
                // If it is "Approved" or "In Progress", hide the delete button so the Admin doesn't accidentally break an active job!
                btnDeleteAppt.setVisibility(View.GONE);
            }
        }

        appointmentsContainer.addView(cardView);
    }
}