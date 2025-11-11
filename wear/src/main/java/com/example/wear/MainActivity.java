package com.example.wear;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Main Activity for Wear OS app
 * Displays status and manages permissions
 */
public class MainActivity extends Activity {

    private static final String TAG = "WearMainActivity";
    private static final int PERMISSION_REQUEST_CODE = 1;

    private TextView statusText;
    private TextView heartRateText;
    private SensorManager sensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "MainActivity created");

        // Initialize views
        statusText = findViewById(R.id.status_text);
        heartRateText = findViewById(R.id.heart_rate_text);

        // Check for heart rate sensor
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        checkHeartRateSensor();

        // Request permissions
        requestBodySensorsPermission();

        // Update status
        updateStatus("Waiting for phone connection...");
    }

    private void checkHeartRateSensor() {
        Sensor heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

        if (heartRateSensor != null) {
            String info = "Heart Rate Sensor Found:\n" +
                    heartRateSensor.getName() + "\n" +
                    "Vendor: " + heartRateSensor.getVendor();
            heartRateText.setText(info);
            Log.d(TAG, info);
        } else {
            String error = "No heart rate sensor available";
            heartRateText.setText(error);
            Log.e(TAG, error);
            Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        }
    }

    private void requestBodySensorsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
                != PackageManager.PERMISSION_GRANTED) {

            Log.d(TAG, "Requesting BODY_SENSORS permission");

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.BODY_SENSORS},
                    PERMISSION_REQUEST_CODE
            );
        } else {
            Log.d(TAG, "BODY_SENSORS permission already granted");
            updateStatus("Ready to monitor heart rate");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "BODY_SENSORS permission granted");
                updateStatus("Ready to monitor heart rate");
                Toast.makeText(this, "Permission granted. Ready to monitor!", Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "BODY_SENSORS permission denied");
                updateStatus("Permission denied - cannot monitor heart rate");
                Toast.makeText(this, "Permission required for heart rate monitoring", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updateStatus(String status) {
        Log.d(TAG, "Status: " + status);
        statusText.setText(status);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "MainActivity resumed");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "MainActivity paused");
    }
}