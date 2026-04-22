package com.example.motosync;

import android.content.Intent;
import android.content.SharedPreferences;
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

public class MyOrdersActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private LinearLayout ordersContainer;
    private DatabaseReference mAppointmentsRef;
    private String savedName; // Identifies the logged-in user

    // Filters
    private TextView filterAll, filterPending, filterCompleted, filterCancelled;
    private String currentFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_orders);

        drawerLayout = findViewById(R.id.drawerLayout);
        ordersContainer = findViewById(R.id.ordersContainer);
        mAppointmentsRef = FirebaseDatabase.getInstance().getReference("Appointments");

        // Fetch User Identity
        SharedPreferences prefs = getSharedPreferences("MotoSyncPrefs", MODE_PRIVATE);
        savedName = prefs.getString("FULL_NAME", "Customer");

        // Filter Mapping
        filterAll = findViewById(R.id.filterAll);
        filterPending = findViewById(R.id.filterPending);
        filterCompleted = findViewById(R.id.filterCompleted);
        filterCancelled = findViewById(R.id.filterCancelled);

        setupFilters();
        setupSidebar();
        fetchMyOrders();
    }

    private void fetchMyOrders() {
        mAppointmentsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (ordersContainer == null) return;
                ordersContainer.removeAllViews();
                boolean hasOrders = false;

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Appointment appt = ds.getValue(Appointment.class);
                    // Match the appointment to the logged-in user
                    if (appt != null && appt.customerName != null && appt.customerName.equals(savedName)) {

                        // Apply the currently selected filter
                        boolean matchFilter = false;
                        if (currentFilter.equals("All")) matchFilter = true;
                        else if (currentFilter.equals("Pending") && appt.status.equals("Pending")) matchFilter = true;
                        else if (currentFilter.equals("Completed") && appt.status.equals("Completed")) matchFilter = true;
                        else if (currentFilter.equals("Cancelled") && (appt.status.equals("Declined") || appt.status.equals("Cancelled"))) matchFilter = true;

                        if (matchFilter) {
                            addOrderCard(appt);
                            hasOrders = true;
                        }
                    }
                }

                if (!hasOrders) {
                    TextView noData = new TextView(MyOrdersActivity.this);
                    noData.setText("No orders found for this category.");
                    noData.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));
                    noData.setPadding(0, 32, 0, 0);
                    ordersContainer.addView(noData);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MyOrdersActivity.this, "Failed to load orders.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addOrderCard(Appointment appt) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_my_order, ordersContainer, false);

        TextView tvVehicle = cardView.findViewById(R.id.tvOrderVehicle);
        TextView tvService = cardView.findViewById(R.id.tvOrderService);
        TextView tvDate = cardView.findViewById(R.id.tvOrderDate);
        TextView tvStatus = cardView.findViewById(R.id.tvOrderStatus);

        if (tvVehicle != null) tvVehicle.setText(appt.vehicleDetails != null ? appt.vehicleDetails : "Unknown Vehicle");
        if (tvService != null) tvService.setText(appt.serviceType != null ? appt.serviceType : "Unknown Service");
        if (tvDate != null) tvDate.setText(appt.date != null ? appt.date : "Date TBD");

        if (tvStatus != null && appt.status != null) {
            tvStatus.setText(appt.status);

            // Dynamic badge colors!
            if (appt.status.equals("Pending")) {
                tvStatus.setBackgroundResource(R.drawable.bg_badge_pending);
            } else if (appt.status.equals("Approved")) {
                tvStatus.setBackgroundResource(R.drawable.bg_badge_purple);
            } else if (appt.status.equals("Completed")) {
                tvStatus.setBackgroundResource(R.drawable.bg_badge_completed);
            } else if (appt.status.equals("Declined") || appt.status.equals("Cancelled")) {
                tvStatus.setBackgroundResource(R.drawable.bg_badge_cancelled);
            }
        }

        ordersContainer.addView(cardView);
    }

    private void setupFilters() {
        View.OnClickListener filterListener = v -> {
            // Reset all styles to gray outline
            filterAll.setBackgroundResource(R.drawable.bg_button_dark);
            filterAll.setTextColor(getResources().getColor(R.color.text_primary, getTheme()));
            filterPending.setBackgroundResource(R.drawable.bg_button_dark);
            filterPending.setTextColor(getResources().getColor(R.color.text_primary, getTheme()));
            filterCompleted.setBackgroundResource(R.drawable.bg_button_dark);
            filterCompleted.setTextColor(getResources().getColor(R.color.text_primary, getTheme()));
            filterCancelled.setBackgroundResource(R.drawable.bg_button_dark);
            filterCancelled.setTextColor(getResources().getColor(R.color.text_primary, getTheme()));

            // Highlight the clicked tab in bright teal/menu active
            TextView clicked = (TextView) v;
            clicked.setBackgroundResource(R.drawable.bg_menu_active);
            clicked.setTextColor(getResources().getColor(R.color.bg_dark, getTheme())); // Black text

            // Set current filter and reload data
            currentFilter = clicked.getText().toString();
            fetchMyOrders();
        };

        if (filterAll != null) filterAll.setOnClickListener(filterListener);
        if (filterPending != null) filterPending.setOnClickListener(filterListener);
        if (filterCompleted != null) filterCompleted.setOnClickListener(filterListener);
        if (filterCancelled != null) filterCancelled.setOnClickListener(filterListener);
    }

    private void setupSidebar() {
        ImageView btnMenu = findViewById(R.id.btnMenu);
        if (btnMenu != null) btnMenu.setOnClickListener(v -> {
            if (drawerLayout != null) drawerLayout.openDrawer(GravityCompat.START);
        });

        // Set Name
        TextView tvSidebarName = findViewById(R.id.tvSidebarName);
        if (tvSidebarName != null) tvSidebarName.setText(savedName);

        // Navigation (Safe checks)
        View navDashboard = findViewById(R.id.navDashboard);
        if (navDashboard != null) navDashboard.setOnClickListener(v -> { startActivity(new Intent(this, MainActivity.class)); finish(); });

        View navBookService = findViewById(R.id.navBookService);
        if (navBookService != null) navBookService.setOnClickListener(v -> { startActivity(new Intent(this, BookingActivity.class)); finish(); });

        View navMyVehicles = findViewById(R.id.navMyVehicles);
        if (navMyVehicles != null) navMyVehicles.setOnClickListener(v -> { startActivity(new Intent(this, VehiclesActivity.class)); finish(); });

        View navMyOrders = findViewById(R.id.navMyOrders);
        if (navMyOrders != null) navMyOrders.setOnClickListener(v -> { if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START); });

        View navMyInvoices = findViewById(R.id.navMyInvoices);
        if (navMyInvoices != null) navMyInvoices.setOnClickListener(v -> { startActivity(new Intent(this, InvoicesActivity.class)); finish(); });

        View btnLogoutMenu = findViewById(R.id.btnLogoutMenu);
        if (btnLogoutMenu != null) btnLogoutMenu.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}