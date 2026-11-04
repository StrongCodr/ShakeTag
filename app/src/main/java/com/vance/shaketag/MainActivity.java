package com.vance.shaketag;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private boolean isShakeModeOn = false;

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
}