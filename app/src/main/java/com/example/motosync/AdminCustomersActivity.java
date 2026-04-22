package com.example.motosync;

import android.app.AlertDialog;
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

public class AdminCustomersActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private DatabaseReference mDatabase;
    private LinearLayout customersContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_customers);

        drawerLayout = findViewById(R.id.drawerLayout);
        customersContainer = findViewById(R.id.customersContainer);
        ImageView btnMenu = findViewById(R.id.btnMenu);

        // Target the Users node in Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference("Users");

        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }

        fetchCustomers();
    }

    private void fetchCustomers() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                customersContainer.removeAllViews(); // Clear existing views to prevent duplicates
                boolean hasClients = false;

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String role = userSnapshot.child("role").getValue(String.class);

                    // Only show users with the role "customer" or "client"
                    if (role != null && (role.equals("customer") || role.equals("client"))) {
                        String id = userSnapshot.getKey(); // The Firebase Node ID
                        String name = userSnapshot.child("fullName").getValue(String.class);
                        String email = userSnapshot.child("email").getValue(String.class);

                        // If you store dates and vehicles in the User node, fetch them here.
                        // Using fallbacks if they are null.
                        String date = userSnapshot.child("registeredDate").getValue(String.class);
                        String vehicle = userSnapshot.child("vehicle").getValue(String.class);

                        addCustomerCardToScreen(
                                id,
                                name != null ? name : "Unknown Name",
                                email != null ? email : "No Email",
                                date != null ? date : "N/A",
                                vehicle != null ? vehicle : "No Vehicle Registered"
                        );
                        hasClients = true;
                    }
                }

                if (!hasClients) {
                    TextView noData = new TextView(AdminCustomersActivity.this);
                    noData.setText("No clients found in the directory.");
                    noData.setTextColor(getResources().getColor(R.color.text_secondary));
                    customersContainer.addView(noData);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminCustomersActivity.this, "Failed to load directory", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addCustomerCardToScreen(String id, String name, String email, String date, String vehicle) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_admin_customer, customersContainer, false);

        ((TextView) cardView.findViewById(R.id.tvCustomerName)).setText(name);
        ((TextView) cardView.findViewById(R.id.tvCustomerId)).setText("ID: " + id);
        ((TextView) cardView.findViewById(R.id.tvCustomerEmail)).setText("Email: " + email);
        ((TextView) cardView.findViewById(R.id.tvRegisteredDate)).setText("Registered: " + date);
        ((TextView) cardView.findViewById(R.id.tvCustomerVehicle)).setText("Vehicle: " + vehicle);

        TextView btnDelete = cardView.findViewById(R.id.btnDeleteCustomer);

        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(AdminCustomersActivity.this)
                    .setTitle("Delete Client")
                    .setMessage("Are you sure you want to permanently remove " + name + " from the directory? This action cannot be undone.")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        // Delete the user from the Firebase Database
                        mDatabase.child(id).removeValue()
                                .addOnSuccessListener(aVoid -> Toast.makeText(AdminCustomersActivity.this, "Client deleted successfully", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(AdminCustomersActivity.this, "Error deleting client: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        customersContainer.addView(cardView);
    }
}