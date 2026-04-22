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

public class AdminInventoryActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private LinearLayout inventoryListContainer;
    private DatabaseReference mDatabase;

    // Metric TextViews
    private TextView tvTotalProducts, tvTotalStock, tvTotalValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_inventory);

        drawerLayout = findViewById(R.id.drawerLayout);
        inventoryListContainer = findViewById(R.id.inventoryListContainer);
        tvTotalProducts = findViewById(R.id.tvTotalProducts);
        tvTotalStock = findViewById(R.id.tvTotalStock);
        tvTotalValue = findViewById(R.id.tvTotalValue);

        mDatabase = FirebaseDatabase.getInstance().getReference("Inventory");

        ImageView btnMenu = findViewById(R.id.btnMenu);
        if (btnMenu != null) btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        ImageView btnAddItem = findViewById(R.id.btnAddItem);
        if (btnAddItem != null) {
            btnAddItem.setOnClickListener(v -> {
                // Generates a test item with the new "Category" parameter
                String newId = mDatabase.push().getKey();
                InventoryItem dummyItem = new InventoryItem(newId, "Pirelli Diablo Tire", "Tires", 4500.00, 2);
                if(newId != null) mDatabase.child(newId).setValue(dummyItem);
                Toast.makeText(this, "Test Item Added!", Toast.LENGTH_SHORT).show();
            });
        }

        setupSidebar();
        fetchInventory();
    }

    private void fetchInventory() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                inventoryListContainer.removeAllViews();

                int totalProducts = 0;
                int totalStock = 0;
                double totalValue = 0.0;

                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    InventoryItem item = itemSnapshot.getValue(InventoryItem.class);
                    if (item != null) {
                        // Calculate Metrics
                        totalProducts++;
                        totalStock += item.stockCount;
                        totalValue += (item.itemPrice * item.stockCount);

                        addItemToList(item);
                    }
                }

                // Update Metric UI Dashboard Cards
                tvTotalProducts.setText(String.valueOf(totalProducts));
                tvTotalStock.setText(String.valueOf(totalStock));
                tvTotalValue.setText(String.format("₱ %,.2f", totalValue));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminInventoryActivity.this, "Failed to load inventory.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addItemToList(InventoryItem item) {
        View rowView = LayoutInflater.from(this).inflate(R.layout.item_admin_inventory_row, inventoryListContainer, false);

        TextView tvName = rowView.findViewById(R.id.tvRowName);
        TextView tvCategory = rowView.findViewById(R.id.tvRowCategory);
        TextView tvPrice = rowView.findViewById(R.id.tvRowPrice);
        TextView tvStock = rowView.findViewById(R.id.tvRowStock);
        TextView btnRestock = rowView.findViewById(R.id.btnRowRestock);

        tvName.setText(item.itemName != null ? item.itemName : "Unknown");
        tvCategory.setText("Category: " + (item.category != null ? item.category : "N/A"));
        tvPrice.setText(String.format("₱ %,.2f", item.itemPrice));

        // Highlight low stock in red
        if (item.stockCount <= 5) {
            tvStock.setText("Stock: " + item.stockCount + " (Low!)");
            tvStock.setTextColor(getResources().getColor(R.color.danger_red, getTheme()));
        } else {
            tvStock.setText("Stock: " + item.stockCount);
            tvStock.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));
        }

        // Restock Button Logic
        btnRestock.setOnClickListener(v -> {
            int newStock = item.stockCount + 5;
            mDatabase.child(item.itemId).child("stockCount").setValue(newStock)
                    .addOnSuccessListener(aVoid -> Toast.makeText(AdminInventoryActivity.this, "Restocked +5", Toast.LENGTH_SHORT).show());
        });

        inventoryListContainer.addView(rowView);
    }

    private void setupSidebar() {
        findViewById(R.id.navAdminDashboard).setOnClickListener(v -> { startActivity(new Intent(this, AdminDashboardActivity.class)); finish(); });
        findViewById(R.id.navManageBookings).setOnClickListener(v -> { startActivity(new Intent(this, AdminAppointmentsActivity.class)); finish(); });
        findViewById(R.id.navJobOrders).setOnClickListener(v -> { startActivity(new Intent(this, AdminJobOrderActivity.class)); finish(); });
        findViewById(R.id.navManageCustomers).setOnClickListener(v -> { startActivity(new Intent(this, AdminCustomersActivity.class)); finish(); });
        findViewById(R.id.btnLogoutMenu).setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}