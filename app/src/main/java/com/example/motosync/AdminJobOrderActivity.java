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
    private DatabaseReference mInvoicesRef;

    private Spinner spinnerAppointments;
    private Spinner spinnerMechanic;
    private EditText etJobNotes;
    private EditText etJobCost;
    private LinearLayout jobOrdersContainer;

    private List<Appointment> approvedAppointmentsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_job_order);

        mAppointmentsRef = FirebaseDatabase.getInstance().getReference("Appointments");
        mJobOrdersRef = FirebaseDatabase.getInstance().getReference("JobOrders");
        mInvoicesRef = FirebaseDatabase.getInstance().getReference("Invoices");

        drawerLayout = findViewById(R.id.drawerLayout);
        ImageView btnMenu = findViewById(R.id.btnMenu);
        spinnerAppointments = findViewById(R.id.spinnerAppointments);
        spinnerMechanic = findViewById(R.id.spinnerMechanic);
        etJobNotes = findViewById(R.id.etJobNotes);
        etJobCost = findViewById(R.id.etJobCost);
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

        // Open Menu Safely
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.openDrawer(GravityCompat.START);
            });
        }

        // --- SAFE SIDEBAR NAVIGATION ---
        View navDashboard = findViewById(R.id.navAdminDashboard);
        if (navDashboard != null) navDashboard.setOnClickListener(v -> { startActivity(new Intent(this, AdminDashboardActivity.class)); finish(); });

        View navBookings = findViewById(R.id.navManageBookings);
        if (navBookings != null) navBookings.setOnClickListener(v -> { startActivity(new Intent(this, AdminAppointmentsActivity.class)); finish(); });

        View navJobOrders = findViewById(R.id.navJobOrders);
        if (navJobOrders != null) navJobOrders.setOnClickListener(v -> {
            if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
        });

        View navCustomers = findViewById(R.id.navManageCustomers);
        if (navCustomers != null) navCustomers.setOnClickListener(v -> { startActivity(new Intent(this, AdminCustomersActivity.class)); finish(); });

        View navServices = findViewById(R.id.navManageServices);
        if (navServices != null) navServices.setOnClickListener(v -> { startActivity(new Intent(this, AdminInventoryActivity.class)); finish(); });

        View navReports = findViewById(R.id.navManageReports);
        if (navReports != null) navReports.setOnClickListener(v -> { startActivity(new Intent(this, AdminInvoicesActivity.class)); finish(); });

        View btnLogout = findViewById(R.id.btnLogoutMenu);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        // --- LOAD MECHANICS ---
        String[] mechanics = {"Select Mechanic...", "John (Engine Specialist)", "Mike (Electrical)", "Alex (General Service)"};
        ArrayAdapter<String> mechAdapter = new ArrayAdapter<>(this, R.layout.item_spinner, mechanics);
        mechAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMechanic.setAdapter(mechAdapter);

        // --- FETCH DATA ---
        loadApprovedAppointments();
        fetchJobOrders();

        // --- SUBMIT JOB ORDER LOGIC ---
        if (btnCreateJobOrder != null) {
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
    }

    private void loadApprovedAppointments() {
        // Removed the strict Firebase filter so it pulls all appointments first
        mAppointmentsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                approvedAppointmentsList.clear();
                List<String> displayStrings = new ArrayList<>();
                displayStrings.add("Select an appointment...");

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Appointment appt = ds.getValue(Appointment.class);

                    // SAFELY CHECK STATUS: Allow both "Pending" and "Approved"
                    if (appt != null && appt.status != null) {
                        if (appt.status.equals("Approved") || appt.status.equals("Pending")) {

                            approvedAppointmentsList.add(appt);

                            String name = appt.customerName != null ? appt.customerName : "Unknown Client";
                            String service = appt.serviceType != null ? appt.serviceType : "Unknown Service";
                            String date = appt.date != null ? appt.date : "Date TBD";

                            // Added a little tag so you know if it's Pending or Approved in the dropdown!
                            String tag = appt.status.equals("Pending") ? "[PENDING] " : "[APPROVED] ";

                            displayStrings.add(tag + name + " - " + service + " (" + date + ")");
                        }
                    }
                }

                ArrayAdapter<String> apptAdapter = new ArrayAdapter<>(AdminJobOrderActivity.this, R.layout.item_spinner, displayStrings);
                apptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                if (spinnerAppointments != null) {
                    spinnerAppointments.setAdapter(apptAdapter);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void createJobOrderAndInvoice(Appointment appt, String mechanic, String cost, String notes) {
        String jobOrderId = mJobOrdersRef.push().getKey();
        String invoiceId = mInvoicesRef.push().getKey();

        // 1. Build Job Order Data Safely
        HashMap<String, Object> jobData = new HashMap<>();
        jobData.put("jobOrderId", jobOrderId);
        jobData.put("invoiceId", invoiceId);
        jobData.put("appointmentId", appt.appointmentId);
        jobData.put("customerName", appt.customerName != null ? appt.customerName : "Unknown");
        jobData.put("serviceType", appt.serviceType != null ? appt.serviceType : "Unknown Service");
        jobData.put("assignedMechanic", mechanic);
        jobData.put("cost", cost);
        jobData.put("jobNotes", notes.isEmpty() ? "None" : notes);
        jobData.put("status", "In Progress");

        // 2. Build Invoice Data Safely
        HashMap<String, Object> invoiceData = new HashMap<>();
        invoiceData.put("invoiceId", invoiceId);
        invoiceData.put("jobOrderId", jobOrderId);
        invoiceData.put("customerName", appt.customerName != null ? appt.customerName : "Unknown");
        invoiceData.put("customerEmail", appt.customerEmail != null ? appt.customerEmail : "No Email");
        invoiceData.put("serviceType", appt.serviceType != null ? appt.serviceType : "Unknown Service");
        invoiceData.put("amount", cost);
        invoiceData.put("status", "Unpaid");

        if (jobOrderId != null && invoiceId != null) {
            mJobOrdersRef.child(jobOrderId).setValue(jobData);
            mInvoicesRef.child(invoiceId).setValue(invoiceData).addOnSuccessListener(aVoid -> {
                if (appt.appointmentId != null) {
                    mAppointmentsRef.child(appt.appointmentId).child("status").setValue("In Progress");
                }
                Toast.makeText(this, "Job Order & Invoice Created!", Toast.LENGTH_SHORT).show();

                etJobCost.setText("");
                etJobNotes.setText("");
                spinnerMechanic.setSelection(0);
                spinnerAppointments.setSelection(0);
            });
        }
    }

    private void fetchJobOrders() {
        mJobOrdersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (jobOrdersContainer == null) return;
                jobOrdersContainer.removeAllViews();

                if (!snapshot.exists()) {
                    TextView noData = new TextView(AdminJobOrderActivity.this);
                    noData.setText("No active job orders right now.");
                    noData.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));
                    jobOrdersContainer.addView(noData);
                    return;
                }

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String jobId = ds.child("jobOrderId").getValue(String.class);
                    String invoiceId = ds.child("invoiceId").getValue(String.class);
                    String apptId = ds.child("appointmentId").getValue(String.class);

                    // --- FIX 2: PREVENT CARDS FROM SHOWING NULL ---
                    String custName = ds.child("customerName").getValue(String.class);
                    String service = ds.child("serviceType").getValue(String.class);
                    String mechanic = ds.child("assignedMechanic").getValue(String.class);
                    String cost = ds.child("cost").getValue(String.class);
                    String notes = ds.child("jobNotes").getValue(String.class);
                    String status = ds.child("status").getValue(String.class);

                    custName = custName != null ? custName : "Unknown";
                    service = service != null ? service : "Unknown Service";
                    mechanic = mechanic != null ? mechanic : "Unassigned";
                    cost = cost != null ? cost : "0.00";
                    notes = notes != null ? notes : "None";
                    status = status != null ? status : "In Progress";

                    addJobOrderCardToScreen(jobId, invoiceId, apptId, custName, service, mechanic, cost, notes, status);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addJobOrderCardToScreen(String jobId, String invoiceId, String apptId, String custName, String service, String mechanic, String cost, String notes, String status) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_admin_job_order, jobOrdersContainer, false);

        TextView tvService = cardView.findViewById(R.id.tvJobService);
        TextView tvCustomer = cardView.findViewById(R.id.tvJobCustomer);
        TextView tvMechanic = cardView.findViewById(R.id.tvJobMechanic);
        TextView tvCost = cardView.findViewById(R.id.tvJobCost);
        TextView tvNotes = cardView.findViewById(R.id.tvJobNotes);
        TextView tvStatus = cardView.findViewById(R.id.tvJobStatus);

        if (tvService != null) tvService.setText(service);
        if (tvCustomer != null) tvCustomer.setText("Customer: " + custName);
        if (tvMechanic != null) tvMechanic.setText("Mechanic: " + mechanic);
        if (tvCost != null) tvCost.setText("Cost: ₱" + cost);
        if (tvNotes != null) tvNotes.setText("Notes: " + notes);

        if (tvStatus != null) {
            tvStatus.setText(status);
            switch (status) {
                case "Completed": tvStatus.setBackgroundResource(R.drawable.bg_badge_completed); break;
                case "Cancelled": tvStatus.setBackgroundResource(R.drawable.bg_badge_cancelled); break;
                case "On Hold": tvStatus.setBackgroundResource(R.drawable.bg_badge_purple); break;
                case "Pending": tvStatus.setBackgroundResource(R.drawable.bg_badge_pending); break;
                default: tvStatus.setBackgroundResource(R.drawable.bg_badge_green); break;
            }
        }

        // Update Status Logic
        LinearLayout btnUpdateStatus = cardView.findViewById(R.id.btnUpdateStatus);
        if (btnUpdateStatus != null) {
            btnUpdateStatus.setOnClickListener(v -> {
                String[] statusOptions = {"Pending", "In Progress", "On Hold", "Completed", "Cancelled"};
                new AlertDialog.Builder(this)
                        .setTitle("Update Job Status")
                        .setItems(statusOptions, (dialog, which) -> {
                            String selectedStatus = statusOptions[which];
                            if (jobId != null) mJobOrdersRef.child(jobId).child("status").setValue(selectedStatus);
                            if (apptId != null) mAppointmentsRef.child(apptId).child("status").setValue(selectedStatus);
                            Toast.makeText(AdminJobOrderActivity.this, "Status updated to " + selectedStatus, Toast.LENGTH_SHORT).show();
                        }).show();
            });
        }

        // Edit Cost Logic
        LinearLayout btnEditCost = cardView.findViewById(R.id.btnEditCost);
        if (btnEditCost != null) {
            btnEditCost.setOnClickListener(v -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Edit Job Cost (₱)");

                final EditText input = new EditText(this);
                input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
                input.setText(cost);
                int padding = (int) (20 * getResources().getDisplayMetrics().density);
                input.setPadding(padding, padding, padding, padding);
                builder.setView(input);

                builder.setPositiveButton("Save", (dialog, which) -> {
                    String newCost = input.getText().toString().trim();
                    if (!newCost.isEmpty() && jobId != null) {
                        mJobOrdersRef.child(jobId).child("cost").setValue(newCost);
                        if (invoiceId != null) mInvoicesRef.child(invoiceId).child("amount").setValue(newCost);
                        Toast.makeText(AdminJobOrderActivity.this, "Cost updated successfully!", Toast.LENGTH_SHORT).show();
                    }
                });
                builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
                builder.show();
            });
        }

        // Delete Logic
        LinearLayout btnDeleteJob = cardView.findViewById(R.id.btnDeleteJob);
        if (btnDeleteJob != null) {
            btnDeleteJob.setOnClickListener(v -> {
                if (jobId != null) mJobOrdersRef.child(jobId).removeValue();
                if (invoiceId != null) mInvoicesRef.child(invoiceId).removeValue();
                if (apptId != null) mAppointmentsRef.child(apptId).child("status").setValue("Approved");
                Toast.makeText(AdminJobOrderActivity.this, "Job Order Deleted", Toast.LENGTH_SHORT).show();
            });
        }

        jobOrdersContainer.addView(cardView);
    }
}