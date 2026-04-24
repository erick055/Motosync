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

public class AdminHistoryActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private LinearLayout adminHistoryContainer;
    private DatabaseReference mJobOrdersRef;
    private ValueEventListener historyListener;

    // This string builder will collect all the data into a spreadsheet format
    private StringBuilder csvDataBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_history);

        drawerLayout = findViewById(R.id.drawerLayout);
        adminHistoryContainer = findViewById(R.id.adminHistoryContainer);
        ImageView btnMenu = findViewById(R.id.btnMenu);
        LinearLayout btnExportCsv = findViewById(R.id.btnExportCsv);

        SharedPreferences prefs = getSharedPreferences("MotoSyncPrefs", MODE_PRIVATE);
        String savedName = prefs.getString("FULL_NAME", "Admin Name");

        TextView tvSidebarName = findViewById(R.id.tvSidebarName);
        if (tvSidebarName != null) tvSidebarName.setText(savedName);

        if (btnMenu != null) btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        LinearLayout navAdminDashboard = findViewById(R.id.navAdminDashboard);
        LinearLayout navManageBookings = findViewById(R.id.navManageBookings);
        LinearLayout navManageCustomers = findViewById(R.id.navManageCustomers);
        LinearLayout navManageServices = findViewById(R.id.navManageServices);
        LinearLayout btnLogoutMenu = findViewById(R.id.btnLogoutMenu);
        LinearLayout navJobOrders = findViewById(R.id.navJobOrders);
        LinearLayout navManageReports = findViewById(R.id.navManageReports);

        // SIDEBAR NAVIGATION
        if (navAdminDashboard != null) {
            navAdminDashboard.setOnClickListener(v -> {
                startActivity(new Intent(this, AdminDashboardActivity.class));
                finish();
            });
        }

        if (navManageBookings != null) {
            navManageBookings.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(this, AdminAppointmentsActivity.class));
            });
        }
        if (navJobOrders != null) {
            navJobOrders.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(this, AdminJobOrderActivity.class));
            });
        }

        if (navManageCustomers != null) {
            navManageCustomers.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
            });
        }

        if (navManageReports != null) {
            navManageReports.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(this, AdminInvoicesActivity.class));
            });
        }

        if (navManageServices != null) {
            navManageServices.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(this, AdminInventoryActivity.class));
            });
        }

        findViewById(R.id.navAdminHistory).setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));

        if(btnLogoutMenu != null) btnLogoutMenu.setOnClickListener(v -> {
            Toast.makeText(AdminHistoryActivity.this, "Logging out...", Toast.LENGTH_SHORT).show();
            // Call the shared AuthUtils method
            AuthUtils.logoutUser(AdminHistoryActivity.this);
        });

        // Initialize our spreadsheet headers!
        csvDataBuilder = new StringBuilder();
        csvDataBuilder.append("Customer Name,Service Type,Assigned Mechanic,Final Cost,Status\n");

        mJobOrdersRef = FirebaseDatabase.getInstance().getReference("JobOrders");
        fetchAdminHistory();

        // THE EXPORT BUTTON LOGIC
        btnExportCsv.setOnClickListener(v -> {
            if (csvDataBuilder.toString().equals("Customer Name,Service Type,Assigned Mechanic,Final Cost,Status\n")) {
                Toast.makeText(this, "No history to export yet!", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("text/csv");
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, "MotoSync_Admin_Service_History");
            sendIntent.putExtra(Intent.EXTRA_TEXT, csvDataBuilder.toString());

            // This opens the bottom menu to share via Gmail, Drive, Copy to Clipboard, etc!
            startActivity(Intent.createChooser(sendIntent, "Export Spreadsheet via..."));
        });
    }

    private void fetchAdminHistory() {
        historyListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                adminHistoryContainer.removeAllViews();
                boolean foundHistory = false;

                // Reset the spreadsheet data in case it refreshes
                csvDataBuilder.setLength(0);
                csvDataBuilder.append("Customer Name,Service Type,Assigned Mechanic,Final Cost,Status\n");

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Boolean isArchived = ds.child("isArchived").getValue(Boolean.class);

                    // ONLY SHOW ARCHIVED/COMPLETED JOBS
                    if (isArchived != null && isArchived) {
                        String custName = ds.child("customerName").getValue(String.class);
                        String service = ds.child("serviceType").getValue(String.class);
                        String mechanic = ds.child("assignedMechanic").getValue(String.class);
                        String cost = ds.child("cost").getValue(String.class);
                        String status = ds.child("status").getValue(String.class);

                        if (custName == null) custName = "Unknown";
                        if (cost == null) cost = "0.00";

                        // 1. Add to the Visual UI
                        View rowView = LayoutInflater.from(AdminHistoryActivity.this).inflate(R.layout.item_service_history_row, adminHistoryContainer, false);
                        ((TextView) rowView.findViewById(R.id.tvRowService)).setText(custName + " - " + service);
                        ((TextView) rowView.findViewById(R.id.tvRowMechanic)).setText(mechanic);
                        ((TextView) rowView.findViewById(R.id.tvRowCost)).setText("₱ " + cost);
                        adminHistoryContainer.addView(rowView);

                        // 2. Append to our hidden Spreadsheet String! (Comma Separated)
                        csvDataBuilder.append(custName).append(",")
                                .append(service).append(",")
                                .append(mechanic).append(",")
                                .append("P").append(cost).append(",")
                                .append(status).append("\n");

                        foundHistory = true;
                    }
                }

                if (!foundHistory) {
                    TextView noData = new TextView(AdminHistoryActivity.this);
                    noData.setText("No Service History Found.");
                    noData.setTextColor(getResources().getColor(R.color.text_secondary));
                    noData.setPadding(16, 32, 16, 32);
                    adminHistoryContainer.addView(noData);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        mJobOrdersRef.addValueEventListener(historyListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mJobOrdersRef != null && historyListener != null) {
            mJobOrdersRef.removeEventListener(historyListener);
        }
    }
}