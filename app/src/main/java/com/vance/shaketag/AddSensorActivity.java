package com.vance.shaketag;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.os.Handler;
import android.os.Looper;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import com.wit.witsdk.Bluetooth.WitBluetoothManager;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import com.wit.witsdk.Device.DeviceManager;
import com.wit.witsdk.Device.Interface.DeviceFindListener;

import com.google.android.material.appbar.MaterialToolbar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AddSensorActivity extends AppCompatActivity implements DeviceFindListener {
    private boolean isScanning = false;
    private static final int SCAN_TIME_MS = 10000;   // WitSDK stops scanning by itself after 10 seconds
    private final Handler scanHandler = new Handler(Looper.getMainLooper());
    private final Runnable rescanTask = this::rescan;
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
        DeviceManager.getInstance().AddDeviceFindListener(this);
    }
    @Override
    // Don't keep scanning when the user leaves this screen
    protected void onPause() {
        super.onPause();

        if (isScanning) {
            stopScan();
        }
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
    public void scanButtonPressed(View view) {
        if (isScanning) {
            stopScan();
            return;
        }

        // SC1 abnormal: Bluetooth access hasn't been allowed yet
        if (!WitBluetoothManager.checkPermissions(this)) {
            WitBluetoothManager.requestPermissions(this);
            addToLog("Allow Bluetooth access, then press Start Scan again. If no pop-up appears, allow Nearby devices for ShakeTag in Settings.");
            return;
        }

        // SC1 abnormal: Bluetooth is switched off
        if (!bluetoothIsOn()) {
            addToLog("Bluetooth is off. Turn it on to scan.");
            return;
        }

        startScan();
    }
    private void startScan() {
        try {
            WitBluetoothManager.getInstance(this).startScan();
            isScanning = true;
            updateScanButton();
            addToLog("Scanning for WitMotion sensors...");
            scanHandler.postDelayed(rescanTask, SCAN_TIME_MS);
        } catch (Exception e) {
            addToLog("Could not start scanning: " + e.getMessage());
        }
    }
    private void rescan() {
        // WitSDK has just stopped by itself, so keep scanning until the user presses Stop Scan
        if (!isScanning) {
            return;
        }
        try {
            WitBluetoothManager.getInstance(this).startScan();
            scanHandler.postDelayed(rescanTask, SCAN_TIME_MS);
        } catch (Exception e) {
            isScanning = false;
            updateScanButton();
            addToLog("Scanning stopped: " + e.getMessage());
        }
    }
    private void stopScan() {
        scanHandler.removeCallbacks(rescanTask);   // cancel the next restart
        try {
            WitBluetoothManager.getInstance(this).stopScan();
        } catch (Exception e) {
            addToLog("Could not stop scanning: " + e.getMessage());
        }
        isScanning = false;
        updateScanButton();
        addToLog("Scan stopped.");
    }
    private boolean bluetoothIsOn() {
        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        BluetoothAdapter adapter = bluetoothManager.getAdapter();
        return adapter != null && adapter.isEnabled();
    }
    private void updateScanButton() {
        Button scanButton = findViewById(R.id.scanButton);
        if (isScanning) {
            scanButton.setText(R.string.stop_scan);
        } else {
            scanButton.setText(R.string.start_scan);
        }
    }

    public void connectSensors(View view) {
        addToLog("Connect pressed.");
        // Connecting the ticked sensors comes later
    }
    private void addToLog(String message) {
        logText.append(message + "\n");
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop WitSDK from telling a closed screen about devices
        DeviceManager.getInstance().RemoveDeviceFindListener(this);
    }
    @Override
    public void onDeviceFound(BluetoothDevice device) {
        addToLog("Found: " + device.getName() + " (" + device.getAddress() + ")");
    }
}