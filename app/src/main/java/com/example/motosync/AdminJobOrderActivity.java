package com.example.motosync;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
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
    private DatabaseReference mInvoicesRef; // NEW: Invoices Database Link

    private Spinner spinnerAppointments;
    private Spinner spinnerMechanic;
    private EditText etJobNotes;
    private EditText etJobCost; // NEW: Cost Input
    private LinearLayout jobOrdersContainer;

    private List<Appointment> approvedAppointmentsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_job_order);

        mAppointmentsRef = FirebaseDatabase.getInstance().getReference("Appointments");
        mJobOrdersRef = FirebaseDatabase.getInstance().getReference("JobOrders");
        mInvoicesRef = FirebaseDatabase.getInstance().getReference("Invoices"); // Initialize

        drawerLayout = findViewById(R.id.drawerLayout);
        ImageView btnMenu = findViewById(R.id.btnMenu);
        spinnerAppointments = findViewById(R.id.spinnerAppointments);
        spinnerMechanic = findViewById(R.id.spinnerMechanic);
        etJobNotes = findViewById(R.id.etJobNotes);
        etJobCost = findViewById(R.id.etJobCost); // Connect ID
        LinearLayout btnCreateJobOrder = findViewById(R.id.btnCreateJobOrder);
        jobOrdersContainer = findViewById(R.id.jobOrdersContainer);

        // --- FETCH LOGGED-IN ADMIN DETAILS ---
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

        // Open Menu
        if (btnMenu != null) btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // --- FULL SIDEBAR NAVIGATION ---
        findViewById(R.id.navAdminDashboard).setOnClickListener(v -> { startActivity(new Intent(this, AdminDashboardActivity.class)); finish(); });
        findViewById(R.id.navManageBookings).setOnClickListener(v -> { startActivity(new Intent(this, AdminAppointmentsActivity.class)); finish(); });
        findViewById(R.id.navJobOrders).setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
        findViewById(R.id.navManageReports).setOnClickListener(v -> startActivity(new Intent(this, AdminInvoicesActivity.class)));
        findViewById(R.id.btnLogoutMenu).setOnClickListener(v -> {
            Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // --- LOAD MECHANICS ---
        String[] mechanics = {"Select Mechanic...", "John (Engine Specialist)", "Mike (Electrical)", "Alex (General Service)"};
        ArrayAdapter<String> mechAdapter = new ArrayAdapter<>(this, R.layout.item_spinner, mechanics);
        mechAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMechanic.setAdapter(mechAdapter);

        // --- FETCH DATA ---
        loadApprovedAppointments();
        fetchJobOrders();

        // --- SUBMIT JOB ORDER LOGIC ---
        btnCreateJobOrder.setOnClickListener(v -> {
            int selectedIndex = spinnerAppointments.getSelectedItemPosition();
            if (selectedIndex <= 0 || approvedAppointmentsList.isEmpty()) {
                Toast.makeText(this, "Please select an approved appointment", Toast.LENGTH_SHORT).show();
                return;
            }
            if (spinnerMechanic.getSelectedItemPosition() == 0) {
                Toast.makeText(this, "Please assign a mechanic", Toast.LENGTH_SHORT).show();
                return;
            }

            String cost = etJobCost.getText().toString().trim();
            if (cost.isEmpty()) {
                Toast.makeText(this, "Please enter an estimated cost", Toast.LENGTH_SHORT).show();
                return;
            }

            Appointment selectedAppt = approvedAppointmentsList.get(selectedIndex - 1);
            String mechanic = spinnerMechanic.getSelectedItem().toString();
            String notes = etJobNotes.getText().toString();

            createJobOrderAndInvoice(selectedAppt, mechanic, cost, notes);
        });
    }

    private void loadApprovedAppointments() {
        mAppointmentsRef.orderByChild("status").equalTo("Approved").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                approvedAppointmentsList.clear();
                List<String> displayStrings = new ArrayList<>();
                displayStrings.add("Select an approved appointment...");

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Appointment appt = ds.getValue(Appointment.class);
                    if (appt != null) {
                        approvedAppointmentsList.add(appt);
                        displayStrings.add(appt.customerName + " - " + appt.serviceType + " (" + appt.date + ")");
                    }
                }

                ArrayAdapter<String> apptAdapter = new ArrayAdapter<>(AdminJobOrderActivity.this, R.layout.item_spinner, displayStrings);
                apptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerAppointments.setAdapter(apptAdapter);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void createJobOrderAndInvoice(Appointment appt, String mechanic, String cost, String notes) {
        String jobOrderId = mJobOrdersRef.push().getKey();
        String invoiceId = mInvoicesRef.push().getKey(); // Create a unique ID for the invoice

        // 1. Build Job Order Data
        HashMap<String, Object> jobData = new HashMap<>();
        jobData.put("jobOrderId", jobOrderId);
        jobData.put("invoiceId", invoiceId); // Link them together!
        jobData.put("appointmentId", appt.appointmentId);
        jobData.put("customerName", appt.customerName);
        jobData.put("serviceType", appt.serviceType);
        jobData.put("assignedMechanic", mechanic);
        jobData.put("cost", cost);
        jobData.put("jobNotes", notes);
        jobData.put("status", "In Progress");

        // 2. Build Invoice Data
        HashMap<String, Object> invoiceData = new HashMap<>();
        invoiceData.put("invoiceId", invoiceId);
        invoiceData.put("jobOrderId", jobOrderId);
        invoiceData.put("customerName", appt.customerName);
        invoiceData.put("customerEmail", appt.customerEmail); // To identify which customer it belongs to
        invoiceData.put("serviceType", appt.serviceType);
        invoiceData.put("amount", cost);
        invoiceData.put("status", "Unpaid");

        if (jobOrderId != null && invoiceId != null) {
            // Save both to Firebase
            mJobOrdersRef.child(jobOrderId).setValue(jobData);
            mInvoicesRef.child(invoiceId).setValue(invoiceData).addOnSuccessListener(aVoid -> {
                mAppointmentsRef.child(appt.appointmentId).child("status").setValue("In Progress");
                Toast.makeText(this, "Job Order & Invoice Created!", Toast.LENGTH_SHORT).show();

                // Clear the form
                etJobCost.setText("");
                etJobNotes.setText("");
                spinnerMechanic.setSelection(0);
            });
        }
    }

    private void fetchJobOrders() {
        mJobOrdersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                jobOrdersContainer.removeAllViews();

                if (!snapshot.exists()) {
                    TextView noData = new TextView(AdminJobOrderActivity.this);
                    noData.setText("No active job orders right now.");
                    noData.setTextColor(getResources().getColor(R.color.text_secondary));
                    jobOrdersContainer.addView(noData);
                    return;
                }

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String jobId = ds.child("jobOrderId").getValue(String.class);
                    String invoiceId = ds.child("invoiceId").getValue(String.class);
                    String apptId = ds.child("appointmentId").getValue(String.class);
                    String custName = ds.child("customerName").getValue(String.class);
                    String service = ds.child("serviceType").getValue(String.class);
                    String mechanic = ds.child("assignedMechanic").getValue(String.class);
                    String cost = ds.child("cost").getValue(String.class);
                    String notes = ds.child("jobNotes").getValue(String.class);
                    String status = ds.child("status").getValue(String.class);

                    if (status == null) status = "In Progress";
                    if (cost == null) cost = "0.00";

                    addJobOrderCardToScreen(jobId, invoiceId, apptId, custName, service, mechanic, cost, notes, status);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addJobOrderCardToScreen(String jobId, String invoiceId, String apptId, String custName, String service, String mechanic, String cost, String notes, String status) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_admin_job_order, jobOrdersContainer, false);

        ((TextView) cardView.findViewById(R.id.tvJobService)).setText(service);
        ((TextView) cardView.findViewById(R.id.tvJobCustomer)).setText("Customer: " + custName);
        ((TextView) cardView.findViewById(R.id.tvJobMechanic)).setText("Mechanic: " + mechanic);
        ((TextView) cardView.findViewById(R.id.tvJobCost)).setText("Cost: ₱" + cost);

        TextView tvNotes = cardView.findViewById(R.id.tvJobNotes);
        if (notes == null || notes.isEmpty()) tvNotes.setText("Notes: None");
        else tvNotes.setText("Notes: " + notes);

        // UI Setup for Status Badge
        TextView tvStatus = cardView.findViewById(R.id.tvJobStatus);
        tvStatus.setText(status);
        switch (status) {
            case "Completed": tvStatus.setBackgroundResource(R.drawable.bg_badge_completed); break;
            case "Cancelled": tvStatus.setBackgroundResource(R.drawable.bg_badge_cancelled); break;
            case "On Hold": tvStatus.setBackgroundResource(R.drawable.bg_badge_purple); break;
            case "Pending": tvStatus.setBackgroundResource(R.drawable.bg_badge_pending); break;
            default: tvStatus.setBackgroundResource(R.drawable.bg_badge_green); break;
        }

        // --- UPDATE STATUS LOGIC ---
        LinearLayout btnUpdateStatus = cardView.findViewById(R.id.btnUpdateStatus);
        btnUpdateStatus.setOnClickListener(v -> {
            String[] statusOptions = {"Pending", "In Progress", "On Hold", "Completed", "Cancelled"};

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Update Job Status");
            builder.setItems(statusOptions, (dialog, which) -> {
                String selectedStatus = statusOptions[which];

                mJobOrdersRef.child(jobId).child("status").setValue(selectedStatus);
                if (apptId != null) mAppointmentsRef.child(apptId).child("status").setValue(selectedStatus);

                Toast.makeText(AdminJobOrderActivity.this, "Status updated to " + selectedStatus, Toast.LENGTH_SHORT).show();
            });
            builder.show();
        });

        // --- EDIT COST LOGIC (NEW) ---
        LinearLayout btnEditCost = cardView.findViewById(R.id.btnEditCost);
        btnEditCost.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Edit Job Cost (₱)");

            // Create a pop-up text field programmatically
            final EditText input = new EditText(this);
            input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            input.setText(cost); // Pre-fill with current cost

            // Add some padding to make it look nicer
            int padding = (int) (20 * getResources().getDisplayMetrics().density);
            input.setPadding(padding, padding, padding, padding);
            builder.setView(input);

            // Save Button
            builder.setPositiveButton("Save", (dialog, which) -> {
                String newCost = input.getText().toString().trim();
                if (!newCost.isEmpty()) {
                    // Update Job Order
                    mJobOrdersRef.child(jobId).child("cost").setValue(newCost);

                    // Automatically Update Connected Invoice!
                    if (invoiceId != null) {
                        mInvoicesRef.child(invoiceId).child("amount").setValue(newCost);
                    }

                    Toast.makeText(AdminJobOrderActivity.this, "Cost updated successfully!", Toast.LENGTH_SHORT).show();
                }
            });

            // Cancel Button
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            builder.show();
        });

        // --- DELETE LOGIC ---
        LinearLayout btnDeleteJob = cardView.findViewById(R.id.btnDeleteJob);
        btnDeleteJob.setOnClickListener(v -> {
            mJobOrdersRef.child(jobId).removeValue();
            if (invoiceId != null) mInvoicesRef.child(invoiceId).removeValue();
            if (apptId != null) mAppointmentsRef.child(apptId).child("status").setValue("Approved");

            Toast.makeText(AdminJobOrderActivity.this, "Job Order & Connected Invoice Deleted", Toast.LENGTH_SHORT).show();
        });

        jobOrdersContainer.addView(cardView);
    }
}