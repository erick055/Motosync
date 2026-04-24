package com.example.motosync;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
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

        // --- THE MENU CRASH FIX ---
        ImageView btnMenu = findViewById(R.id.btnMenu);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                // Now it checks if the drawer exists BEFORE trying to open it!
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(GravityCompat.START);
                } else {
                    Toast.makeText(this, "Error: Drawer layout missing in XML", Toast.LENGTH_SHORT).show();
                }
            });
        }

        ImageView btnAddItem = findViewById(R.id.btnAddItem);
        if (btnAddItem != null) {
            btnAddItem.setOnClickListener(v -> showAddInventoryDialog());
        }

        setupSidebarSafe();
        fetchInventory();
    }

    private void showAddInventoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_inventory, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        EditText etName = dialogView.findViewById(R.id.etInvName);
        EditText etCategory = dialogView.findViewById(R.id.etInvCategory);
        EditText etPrice = dialogView.findViewById(R.id.etInvPrice);
        EditText etStock = dialogView.findViewById(R.id.etInvStock);
        TextView btnCancel = dialogView.findViewById(R.id.btnCancelInv);
        TextView btnSave = dialogView.findViewById(R.id.btnSaveInv);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String category = etCategory.getText().toString().trim();
            String priceStr = etPrice.getText().toString().trim();
            String stockStr = etStock.getText().toString().trim();

            if (name.isEmpty() || category.isEmpty() || priceStr.isEmpty() || stockStr.isEmpty()) {
                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double price = Double.parseDouble(priceStr);
                int stock = Integer.parseInt(stockStr);

                String newId = mDatabase.push().getKey();
                if (newId != null) {
                    InventoryItem newItem = new InventoryItem(newId, name, category, price, stock);
                    mDatabase.child(newId).setValue(newItem)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Product Added Successfully!", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to add product", Toast.LENGTH_SHORT).show());
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show();
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
        // --- FETCH ADMIN NAME FOR SIDEBAR ---
        SharedPreferences prefs = getSharedPreferences("MotoSyncPrefs", MODE_PRIVATE);
        String savedName = prefs.getString("FULL_NAME", "Admin Name");
        String savedRole = prefs.getString("ROLE", "admin");

        TextView tvSidebarName = findViewById(R.id.tvSidebarName);
        TextView tvSidebarRole = findViewById(R.id.tvSidebarRole);

        if (tvSidebarName != null) tvSidebarName.setText(savedName);
        if (tvSidebarRole != null && savedRole.length() > 0) {
            tvSidebarRole.setText(savedRole.substring(0, 1).toUpperCase() + savedRole.substring(1) + " Account");
        }

        // --- NAVIGATION LOGIC ---
        View navDashboard = findViewById(R.id.navAdminDashboard);
        if (navDashboard != null) navDashboard.setOnClickListener(v -> { startActivity(new Intent(this, AdminDashboardActivity.class)); finish(); });

        View navBookings = findViewById(R.id.navManageBookings);
        if (navBookings != null) navBookings.setOnClickListener(v -> { startActivity(new Intent(this, AdminAppointmentsActivity.class)); finish(); });

        // THE TYPO IS FIXED HERE!
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

        LinearLayout navAdminHistory = findViewById(R.id.navAdminHistory);
        if (navAdminHistory != null) navAdminHistory.setOnClickListener(v -> {
            startActivity(new Intent(v.getContext(), AdminHistoryActivity.class));
            finish();
        });

        LinearLayout btnLogoutMenu = findViewById(R.id.btnLogoutMenu);
        if(btnLogoutMenu != null) btnLogoutMenu.setOnClickListener(v -> {
            Toast.makeText(AdminInventoryActivity.this, "Logging out...", Toast.LENGTH_SHORT).show();
            // Call the shared AuthUtils method
            AuthUtils.logoutUser(AdminInventoryActivity.this);
        });
        }

}