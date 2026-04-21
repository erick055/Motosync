package com.example.motosync;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
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

public class AdminAppointmentsActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private DatabaseReference mDatabase;
    private LinearLayout appointmentsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_appointments);

        // UI & Firebase Setup
        drawerLayout = findViewById(R.id.drawerLayout);
        mDatabase = FirebaseDatabase.getInstance().getReference("Appointments");
        appointmentsContainer = findViewById(R.id.appointmentsContainer);
        ImageView btnMenu = findViewById(R.id.btnMenu);

        // --- FETCH AND DISPLAY ADMIN DATA IN SIDEBAR ---
        android.content.SharedPreferences prefs = getSharedPreferences("MotoSyncPrefs", MODE_PRIVATE);
        String savedName = prefs.getString("FULL_NAME", "Admin Name");
        String savedRole = prefs.getString("ROLE", "admin");

        TextView tvSidebarName = findViewById(R.id.tvSidebarName);
        TextView tvSidebarRole = findViewById(R.id.tvSidebarRole);

        if (tvSidebarName != null) tvSidebarName.setText(savedName);
        if (tvSidebarRole != null && savedRole.length() > 0) {
            String displayRole = savedRole.substring(0, 1).toUpperCase() + savedRole.substring(1);
            tvSidebarRole.setText(displayRole + " Account");
        }
        // -----------------------------------

        // Open Menu
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }

        // --- ADMIN SIDEBAR NAVIGATION ---
        LinearLayout navAdminDashboard = findViewById(R.id.navAdminDashboard);
        LinearLayout navManageBookings = findViewById(R.id.navManageBookings);
        LinearLayout navManageCustomers = findViewById(R.id.navManageCustomers);
        LinearLayout navManageServices = findViewById(R.id.navManageServices);
        LinearLayout navManageReports = findViewById(R.id.navManageReports);
        LinearLayout btnLogoutMenu = findViewById(R.id.btnLogoutMenu);

        if (navAdminDashboard != null) {
            navAdminDashboard.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(this, AdminDashboardActivity.class));
                finish();
            });
        }

        // Already on this page, just close drawer
        if (navManageBookings != null) {
            navManageBookings.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
        }

        if (navManageCustomers != null) {
            navManageCustomers.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                Toast.makeText(this, "Opening Customer Directory...", Toast.LENGTH_SHORT).show();
            });
        }

        if (navManageServices != null) {
            navManageServices.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                Toast.makeText(this, "Opening Services & Pricing...", Toast.LENGTH_SHORT).show();
            });
        }

        if (navManageReports != null) {
            navManageReports.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                Toast.makeText(this, "Opening Financial Reports...", Toast.LENGTH_SHORT).show();
            });
        }

        // Handle Logout
        if (btnLogoutMenu != null) {
            btnLogoutMenu.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        // Fetch Data from Firebase
        fetchAppointments();
    }

    // --- FIREBASE LOGIC ---
    private void fetchAppointments() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                appointmentsContainer.removeAllViews(); // Clear the screen to prevent duplicates

                if (!snapshot.exists()) {
                    TextView noData = new TextView(AdminAppointmentsActivity.this);
                    noData.setText("No appointment bookings found.");
                    noData.setTextColor(getResources().getColor(R.color.text_secondary));
                    noData.setTextSize(16f);
                    appointmentsContainer.addView(noData);
                    return;
                }

                for (DataSnapshot apptSnapshot : snapshot.getChildren()) {
                    Appointment appt = apptSnapshot.getValue(Appointment.class);
                    if (appt != null) {
                        addAppointmentCardToScreen(appt);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminAppointmentsActivity.this, "Failed to load data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addAppointmentCardToScreen(Appointment appt) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_admin_appointment, appointmentsContainer, false);

        TextView tvApptService = cardView.findViewById(R.id.tvApptService);
        TextView tvApptCustomer = cardView.findViewById(R.id.tvApptCustomer);
        TextView tvApptVehicle = cardView.findViewById(R.id.tvApptVehicle);
        TextView tvApptDate = cardView.findViewById(R.id.tvApptDate);
        TextView tvApptStatus = cardView.findViewById(R.id.tvApptStatus);
        TextView btnApprove = cardView.findViewById(R.id.btnApprove);
        TextView btnDecline = cardView.findViewById(R.id.btnDecline);
        LinearLayout layoutActionButtons = cardView.findViewById(R.id.layoutActionButtons);

        tvApptService.setText(appt.serviceType);
        tvApptCustomer.setText("Customer: " + appt.customerName);
        tvApptVehicle.setText("Vehicle: " + appt.vehicleDetails);
        tvApptDate.setText("Date: " + appt.date);
        tvApptStatus.setText(appt.status);

        // Hide buttons if not Pending
        if (!"Pending".equals(appt.status)) {
            layoutActionButtons.setVisibility(View.GONE);
        }

        // Handle Approve
        btnApprove.setOnClickListener(v -> {
            mDatabase.child(appt.appointmentId).child("status").setValue("Approved");
            Toast.makeText(this, "Appointment Approved!", Toast.LENGTH_SHORT).show();
        });

        // Handle Decline
        btnDecline.setOnClickListener(v -> {
            mDatabase.child(appt.appointmentId).child("status").setValue("Declined");
            Toast.makeText(this, "Appointment Declined", Toast.LENGTH_SHORT).show();
        });

        appointmentsContainer.addView(cardView);
    }
}