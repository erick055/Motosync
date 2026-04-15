package com.example.motosync;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Ensure this matches your XML file name

        // Find the custom buttons by their IDs
        LinearLayout btnBookService = findViewById(R.id.btnBookService);
        LinearLayout btnAddVehicle = findViewById(R.id.btnAddVehicle);

        // Set Click Listener for Booking Service
        btnBookService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Use Intent to navigate to the Booking Screen later
                Toast.makeText(MainActivity.this, "Opening Booking Screen...", Toast.LENGTH_SHORT).show();
            }
        });

        // Set Click Listener for Adding Vehicles
        btnAddVehicle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Use Intent to navigate to the Add Vehicle Screen later
                Toast.makeText(MainActivity.this, "Opening Add Vehicle Screen...", Toast.LENGTH_SHORT).show();
            }
        });
    }
}