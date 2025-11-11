package com.example.wear;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Service that monitors heart rate on Wear OS device and sends data to phone
 */
public class HeartRateService extends WearableListenerService implements SensorEventListener {

    private static final String TAG = "HeartRateService";

    // Data paths - must match phone app
    private static final String HEART_RATE_PATH = "/heart_rate";
    private static final String START_PATH = "/start_heart_rate";
    private static final String STOP_PATH = "/stop_heart_rate";

    // Keys for data map
    private static final String KEY_HEART_RATE = "heart_rate";
    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_ACCURACY = "accuracy";

    private SensorManager sensorManager;
    private Sensor heartRateSensor;
    private boolean isMonitoring = false;
    private int lastHeartRate = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "HeartRateService created");

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

        if (heartRateSensor == null) {
            Log.e(TAG, "Heart rate sensor not available on this device");
        } else {
            Log.d(TAG, "Heart rate sensor found: " + heartRateSensor.getName());
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        String path = messageEvent.getPath();
        String message = new String(messageEvent.getData());

        Log.d(TAG, "Message received - Path: " + path + ", Data: " + message);

        if (START_PATH.equals(path)) {
            Log.d(TAG, "Received START command from phone");
            startHeartRateMonitoring();
        } else if (STOP_PATH.equals(path)) {
            Log.d(TAG, "Received STOP command from phone");
            stopHeartRateMonitoring();
        }
    }

    private void startHeartRateMonitoring() {
        if (heartRateSensor == null) {
            Log.e(TAG, "Cannot start monitoring - no heart rate sensor");
            return;
        }

        if (!isMonitoring) {
            isMonitoring = true;
            boolean registered = sensorManager.registerListener(
                    this,
                    heartRateSensor,
                    SensorManager.SENSOR_DELAY_NORMAL
            );

            if (registered) {
                Log.d(TAG, "Heart rate monitoring started successfully");
            } else {
                Log.e(TAG, "Failed to register sensor listener");
                isMonitoring = false;
            }
        } else {
            Log.d(TAG, "Already monitoring heart rate");
        }
    }

    private void stopHeartRateMonitoring() {
        if (isMonitoring) {
            isMonitoring = false;
            sensorManager.unregisterListener(this);
            Log.d(TAG, "Heart rate monitoring stopped");
        } else {
            Log.d(TAG, "Not currently monitoring");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_HEART_RATE && isMonitoring) {
            float heartRateFloat = event.values[0];
            int heartRate = Math.round(heartRateFloat);

            Log.d(TAG, "Heart rate reading: " + heartRate + " BPM (accuracy: " + event.accuracy + ")");

            // Only send if heart rate is valid and different from last reading
            if (heartRate > 0 && heartRate != lastHeartRate) {
                lastHeartRate = heartRate;
                sendHeartRateToPhone(heartRate, event.accuracy);
            } else if (heartRate == 0) {
                Log.d(TAG, "Invalid heart rate reading (0), skipping");
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        String accuracyStr;
        switch (accuracy) {
            case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                accuracyStr = "HIGH";
                break;
            case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                accuracyStr = "MEDIUM";
                break;
            case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                accuracyStr = "LOW";
                break;
            case SensorManager.SENSOR_STATUS_UNRELIABLE:
                accuracyStr = "UNRELIABLE";
                break;
            default:
                accuracyStr = "UNKNOWN";
        }
        Log.d(TAG, "Sensor accuracy changed to: " + accuracyStr);
    }

    private void sendHeartRateToPhone(int heartRate, int accuracy) {
        Log.d(TAG, "Sending heart rate to phone: " + heartRate + " BPM");

        // Create data map with heart rate information
        PutDataMapRequest dataMap = PutDataMapRequest.create(HEART_RATE_PATH);
        DataMap map = dataMap.getDataMap();
        map.putInt(KEY_HEART_RATE, heartRate);
        map.putLong(KEY_TIMESTAMP, System.currentTimeMillis());
        map.putInt(KEY_ACCURACY, accuracy);

        // Create put data request and mark as urgent for immediate delivery
        PutDataRequest request = dataMap.asPutDataRequest();
        request.setUrgent();

        // Send to phone
        DataClient dataClient = Wearable.getDataClient(this);
        dataClient.putDataItem(request)
                .addOnSuccessListener(dataItem -> {
                    Log.d(TAG, "✓ Heart rate sent successfully: " + heartRate + " BPM");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Failed to send heart rate: " + e.getMessage(), e);
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "HeartRateService destroyed");
        stopHeartRateMonitoring();
    }
}