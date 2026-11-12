package com.vance.shaketag;

import android.content.Intent;
import android.view.MenuItem;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import android.os.Bundle;
import android.text.Html;
import android.widget.Button;
import android.view.View;
import android.widget.LinearLayout;
import android.graphics.Typeface;
import android.graphics.Color;
import com.wit.witsdk.Bluetooth.WitBluetoothManager;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.content.ContextCompat;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.text.Html;
import android.widget.PopupMenu;
import android.view.Gravity;

public class MainActivity extends AppCompatActivity {
    private boolean isShakeModeOn = false;
    private LinearLayout sensorList;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        sensorList = findViewById(R.id.sensorList);
        askPermissions();

    }
    private void askPermissions() {
        // Ask for Bluetooth and location as soon as the app opens
        if (!WitBluetoothManager.checkPermissions(this)) {
            WitBluetoothManager.requestPermissions(this);
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        showSensors();
        SensorManager.getInstance().setOnChange(this::showSensors);
    }
    private void showSensors(){
        sensorList.removeAllViews();
        for (Sensor sensor : SensorManager.getInstance().getSensors()) {
            TextView sensorRow = new TextView(this);
            sensorRow.setText(sensor.getName());
            sensorRow.setTextSize(18);
            sensorRow.setText(Html.fromHtml("<b>" + sensor.getName() + "</b> " + sensor.getStatusText(), Html.FROM_HTML_MODE_LEGACY));
            sensorRow.setBackgroundResource(R.drawable.sensor_card);
            sensorRow.setPadding(40, 40, 40, 80);
            sensorRow.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.outline_keyboard_arrow_down_24, 0);
            sensorRow.setOnClickListener(view -> showSensorMenu(view, sensor));
            sensorList.addView(sensorRow);
            sensorRow.setTextColor(Color.BLACK);
        }

        TextView noSensorsText = findViewById(R.id.noSensorsText);
        if (hasSensors()) {
            noSensorsText.setVisibility(View.GONE);
        } else {
            noSensorsText.setVisibility(View.VISIBLE);
        }

    }
    private void showSensorMenu(View card, Sensor sensor) {
        PopupMenu menu = new PopupMenu(this, card);
        menu.setGravity(Gravity.END);
        MenuItem renameOption = menu.getMenu().add(R.string.rename);
        MenuItem deleteOption = menu.getMenu().add(R.string.delete);

        menu.setOnMenuItemClickListener(item -> {
            if (item == renameOption) {
                showRenameBox(sensor);
            }
            return true;
        });
        menu.show();
    }
    private boolean hasSensors(){
        return !SensorManager.getInstance().getSensors().isEmpty();
    }



    public void openAddSensorScreen(View view) {
        Intent goToAddSensor = new Intent(this, AddSensorActivity.class);
        startActivity(goToAddSensor);
    }
    public void toggleShakeMode(View view) {
        // TODO (SC3, SC4): if there are no sensors, show "No sensors registered" and keep Shake Mode off
        isShakeModeOn = !isShakeModeOn;
        updateShakeButton();
        // TODO (SC3): tell SensorController to start or stop highlighting shaken sensors yellow
    }

    @Override
    protected void onPause() {
        super.onPause();
        SensorManager.getInstance().setOnChange(null);
        // SC4: leaving the menu, leaving the app, or the screen turning off all stop Shake Mode
        isShakeModeOn = false;
        updateShakeButton();
    }
    private void updateShakeButton() {
        Button shakeButton = findViewById(R.id.shakeButton);
        if (isShakeModeOn) {
            shakeButton.setText(R.string.shake_mode_on);
        } else {
            shakeButton.setText(R.string.shake_mode_off);
        }

    }

    private void showRenameBox(Sensor sensor) {
        EditText nameInput = new EditText(this);
        nameInput.setText(sensor.getName());
        nameInput.setSingleLine();

        AlertDialog.Builder renameBox = new AlertDialog.Builder(this);
        renameBox.setTitle(R.string.rename_title);
        renameBox.setView(nameInput);
        renameBox.setPositiveButton(R.string.save, null);
        renameBox.setNegativeButton(R.string.cancel, null);
        AlertDialog box = renameBox.show();

        //SC6: only close the box when the name is accepted
        box.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String error = SensorManager.getInstance().renameSensor(sensor, nameInput.getText().toString());
            if (error == null) {
                showSensors();
                box.dismiss();
            } else {
                nameInput.setError(error);
            }
        });
    }
}