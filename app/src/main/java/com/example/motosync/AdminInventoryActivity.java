package com.example.motosync;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
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

        // --- NEW POPUP LOGIC FOR ADD BUTTON ---
        ImageView btnAddItem = findViewById(R.id.btnAddItem);
        if (btnAddItem != null) {
            btnAddItem.setOnClickListener(v -> showAddInventoryDialog());
        }

        setupSidebarSafe();
        fetchInventory();
    }

    // --- POPUP WINDOW METHOD ---
    private void showAddInventoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_inventory, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        // Makes the background transparent so the rounded corners of your XML card show up correctly
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Map the popup inputs
        EditText etName = dialogView.findViewById(R.id.etInvName);
        EditText etCategory = dialogView.findViewById(R.id.etInvCategory);
        EditText etPrice = dialogView.findViewById(R.id.etInvPrice);
        EditText etStock = dialogView.findViewById(R.id.etInvStock);
        TextView btnCancel = dialogView.findViewById(R.id.btnCancelInv);
        TextView btnSave = dialogView.findViewById(R.id.btnSaveInv);

        // Cancel Button closes the popup
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Save Button processes the inputs
        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String category = etCategory.getText().toString().trim();
            String priceStr = etPrice.getText().toString().trim();
            String stockStr = etStock.getText().toString().trim();

            // Safety Check: Ensure no fields are blank
            if (name.isEmpty() || category.isEmpty() || priceStr.isEmpty() || stockStr.isEmpty()) {
                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                // Convert text inputs into proper math numbers
                double price = Double.parseDouble(priceStr);
                int stock = Integer.parseInt(stockStr);

                String newId = mDatabase.push().getKey();
                if (newId != null) {
                    InventoryItem newItem = new InventoryItem(newId, name, category, price, stock);

                    // Push to Firebase Realtime Database
                    mDatabase.child(newId).setValue(newItem)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Product Added Successfully!", Toast.LENGTH_SHORT).show();
                                dialog.dismiss(); // Close popup on success
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to add product", Toast.LENGTH_SHORT).show());
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid number format for Price or Stock", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void fetchInventory() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (inventoryListContainer == null) return;

                inventoryListContainer.removeAllViews();

                int totalProducts = 0;
                int totalStock = 0;
                double totalValue = 0.0;

                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    InventoryItem item = itemSnapshot.getValue(InventoryItem.class);
                    if (item != null) {
                        totalProducts++;
                        totalStock += item.stockCount;
                        totalValue += (item.itemPrice * item.stockCount);
                        addItemToList(item);
                    }
                }

                if (tvTotalProducts != null) tvTotalProducts.setText(String.valueOf(totalProducts));
                if (tvTotalStock != null) tvTotalStock.setText(String.valueOf(totalStock));
                if (tvTotalValue != null) tvTotalValue.setText(String.format("₱ %,.2f", totalValue));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminInventoryActivity.this, "Failed to load inventory.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addItemToList(InventoryItem item) {
        if (inventoryListContainer == null) return;

        View rowView = LayoutInflater.from(this).inflate(R.layout.item_admin_inventory_row, inventoryListContainer, false);

        TextView tvName = rowView.findViewById(R.id.tvRowName);
        TextView tvCategory = rowView.findViewById(R.id.tvRowCategory);
        TextView tvPrice = rowView.findViewById(R.id.tvRowPrice);
        TextView tvStock = rowView.findViewById(R.id.tvRowStock);
        TextView btnRestock = rowView.findViewById(R.id.btnRowRestock);

        // NEW: Map the Delete button
        TextView btnDelete = rowView.findViewById(R.id.btnRowDelete);

        if (tvName != null) tvName.setText(item.itemName != null ? item.itemName : "Unknown");
        if (tvCategory != null) tvCategory.setText("Category: " + (item.category != null ? item.category : "N/A"));
        if (tvPrice != null) tvPrice.setText(String.format("₱ %,.2f", item.itemPrice));

        if (tvStock != null) {
            if (item.stockCount <= 5) {
                tvStock.setText("Stock: " + item.stockCount + " (Low!)");
                tvStock.setTextColor(getResources().getColor(R.color.danger_red, getTheme()));
            } else {
                tvStock.setText("Stock: " + item.stockCount);
                tvStock.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));
            }
        }

        if (btnRestock != null) {
            btnRestock.setOnClickListener(v -> {
                int newStock = item.stockCount + 5;
                if (item.itemId != null) {
                    mDatabase.child(item.itemId).child("stockCount").setValue(newStock)
                            .addOnSuccessListener(aVoid -> Toast.makeText(AdminInventoryActivity.this, "Restocked +5", Toast.LENGTH_SHORT).show());
                }
            });
        }

        // NEW: Delete Button Logic with Confirmation Dialog
        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Delete Product")
                        .setMessage("Are you sure you want to permanently remove " + item.itemName + " from your inventory?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            if (item.itemId != null) {
                                mDatabase.child(item.itemId).removeValue()
                                        .addOnSuccessListener(aVoid -> Toast.makeText(AdminInventoryActivity.this, "Product Deleted", Toast.LENGTH_SHORT).show());
                            }
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .show();
            });
        }

        inventoryListContainer.addView(rowView);
    }

    private void setupSidebarSafe() {
        View navDashboard = findViewById(R.id.navAdminDashboard);
        if (navDashboard != null) navDashboard.setOnClickListener(v -> { startActivity(new Intent(this, AdminDashboardActivity.class)); finish(); });

        View navBookings = findViewById(R.id.navManageBookings);
        if (navBookings != null) navBookings.setOnClickListener(v -> { startActivity(new Intent(this, AdminAppointmentsActivity.class)); finish(); });

        View navJobOrders = findViewById(R.id.navJobOrders);
        if (navJobOrders != null) navJobOrders.setOnClickListener(v -> { startActivity(new Intent(this, AdminJobOrderActivity.class)); finish(); });

        View navCustomers = findViewById(R.id.navManageCustomers);
        if (navCustomers != null) navCustomers.setOnClickListener(v -> { startActivity(new Intent(this, AdminCustomersActivity.class)); finish(); });

        View navServices = findViewById(R.id.navManageServices);
        if (navServices != null) navServices.setOnClickListener(v -> {
            if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
        });

        View navReports = findViewById(R.id.navManageReports);
        if (navReports != null) navReports.setOnClickListener(v -> { startActivity(new Intent(this, AdminInvoicesActivity.class)); finish(); });

        View btnLogout = findViewById(R.id.btnLogoutMenu);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }
}