package com.example.motosync;

import android.content.Intent;
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
import java.util.HashMap;
import java.util.List;

public class AdminJobOrderActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private DatabaseReference mAppointmentsRef;
    private DatabaseReference mJobOrdersRef;
    private Spinner spinnerAppointments;
    private Spinner spinnerMechanic;
    private List<Appointment> pendingAppointmentsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_job_order);

        mAppointmentsRef = FirebaseDatabase.getInstance().getReference("Appointments");
        mJobOrdersRef = FirebaseDatabase.getInstance().getReference("JobOrders");

        drawerLayout = findViewById(R.id.drawerLayout);
        ImageView btnMenu = findViewById(R.id.btnMenu);
        spinnerAppointments = findViewById(R.id.spinnerAppointments);
        spinnerMechanic = findViewById(R.id.spinnerMechanic);
        EditText etJobNotes = findViewById(R.id.etJobNotes);
        LinearLayout btnCreateJobOrder = findViewById(R.id.btnCreateJobOrder);

        if (btnMenu != null) btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Navigation
        findViewById(R.id.navAdminDashboard).setOnClickListener(v -> { startActivity(new Intent(this, AdminDashboardActivity.class)); finish(); });
        findViewById(R.id.navManageBookings).setOnClickListener(v -> { startActivity(new Intent(this, AdminAppointmentsActivity.class)); finish(); });

        // Load Mechanics (Dummy data for now)
        String[] mechanics = {"Select Mechanic...", "John (Engine Specialist)", "Mike (Electrical)", "Alex (General Service)"};
        ArrayAdapter<String> mechAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, mechanics);
        mechAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMechanic.setAdapter(mechAdapter);

        loadPendingAppointments();

        // Submit Logic
        btnCreateJobOrder.setOnClickListener(v -> {
            int selectedIndex = spinnerAppointments.getSelectedItemPosition();
            if (selectedIndex <= 0 || pendingAppointmentsList.isEmpty()) {
                Toast.makeText(this, "Please select a pending appointment", Toast.LENGTH_SHORT).show();
                return;
            }

            if (spinnerMechanic.getSelectedItemPosition() == 0) {
                Toast.makeText(this, "Please assign a mechanic", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get the actual selected appointment (subtract 1 because index 0 is "Select...")
            Appointment selectedAppt = pendingAppointmentsList.get(selectedIndex - 1);
            String mechanic = spinnerMechanic.getSelectedItem().toString();
            String notes = etJobNotes.getText().toString();

            createJobOrder(selectedAppt, mechanic, notes);
        });
    }

    private void loadPendingAppointments() {
        mAppointmentsRef.orderByChild("status").equalTo("Pending").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                pendingAppointmentsList.clear();
                List<String> displayStrings = new ArrayList<>();
                displayStrings.add("Select an incoming appointment..."); // Default placeholder

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Appointment appt = ds.getValue(Appointment.class);
                    if (appt != null) {
                        pendingAppointmentsList.add(appt);
                        displayStrings.add(appt.customerName + " - " + appt.serviceType + " (" + appt.date + ")");
                    }
                }

                ArrayAdapter<String> apptAdapter = new ArrayAdapter<>(AdminJobOrderActivity.this, android.R.layout.simple_spinner_item, displayStrings);
                apptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerAppointments.setAdapter(apptAdapter);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void createJobOrder(Appointment appt, String mechanic, String notes) {
        String jobOrderId = mJobOrdersRef.push().getKey();

        // Fast-track saving using a HashMap instead of building a whole new Model class
        HashMap<String, Object> jobData = new HashMap<>();
        jobData.put("jobOrderId", jobOrderId);
        jobData.put("appointmentId", appt.appointmentId);
        jobData.put("customerName", appt.customerName);
        jobData.put("serviceType", appt.serviceType);
        jobData.put("assignedMechanic", mechanic);
        jobData.put("jobNotes", notes);
        jobData.put("status", "In Progress");

        if (jobOrderId != null) {
            mJobOrdersRef.child(jobOrderId).setValue(jobData).addOnSuccessListener(aVoid -> {
                // Update original appointment status
                mAppointmentsRef.child(appt.appointmentId).child("status").setValue("Assigned");
                Toast.makeText(this, "Job Order Created Successfully!", Toast.LENGTH_LONG).show();
                finish();
                startActivity(getIntent()); // Refresh page to clear form
            });
        }
    }
}