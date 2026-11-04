package com.vance.shaketag;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AddSensorActivity extends AppCompatActivity {
    private boolean isScanning = false;
    private TextView logText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_sensor);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        setUpToolbar();
        logText = findViewById(R.id.logText);
    }
    private void setUpToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.add_sensor_title);
        toolbar.setNavigationIcon(R.drawable.outline_arrow_back_24);
        toolbar.setNavigationIconTint(Color.WHITE);
        toolbar.setNavigationOnClickListener(this::goBack);
    }

    public void goBack(View view) {
        finish();
    }
    public void startOrStopScan(View view) {
        Button scanButton = (Button) view;
        isScanning = !isScanning;

        if (isScanning) {
            scanButton.setText(R.string.stop_scan);
            addToLog("Scanning for WitMotion sensors...");
        } else {
            scanButton.setText(R.string.start_scan);
            addToLog("Scan stopped.");
        }
    }

    public void connectSensors(View view) {
        addToLog("Connect pressed.");
        // Connecting the ticked sensors comes later
    }
    private void addToLog(String message) {
        logText.append(message + "\n");
    }
}