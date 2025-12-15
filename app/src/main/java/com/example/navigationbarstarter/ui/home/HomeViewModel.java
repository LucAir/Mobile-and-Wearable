package com.example.navigationbarstarter.ui.home;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.navigationbarstarter.R;
import com.example.navigationbarstarter.data.CSVHeartbeatSimulator;
import com.example.navigationbarstarter.data.HeartRateVariability;
import com.example.navigationbarstarter.database.AppDatabase;
import com.example.navigationbarstarter.database.UserData;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeViewModel extends AndroidViewModel {

    // Simulation Data
    private final MutableLiveData<Integer> heartRate = new MutableLiveData<>();
    private CSVHeartbeatSimulator heartbeatSimulator;

    // Timer State
    private boolean isTimerRunning = false;
    private long accumulatedTime = 0; // Time tracked before the current "start"
    private long lastStartTime = 0;  // Timestamp of the latest "start"

    // Break Logic State
    private long lastBreakDismissTime = 0;
    private static final long BREAK_SNOOZE_DURATION_MS = 5 * 60 * 1000; // 5 Minutes

    // Events for Fragment
    private final MutableLiveData<Boolean> showBreakDialogEvent = new MutableLiveData<>(false);

    //Variable used for HRV
    private final List<Integer> heartRateWindow = new ArrayList<>();
    private static final int HRV_WINDOW_SIZE = 20;
    private final MutableLiveData<Double> hrvLive = new MutableLiveData<>();

    // Default is true (Notifications Allowed).
    // If false -> Red oblique line, and DND will activate on Play.
    private MutableLiveData<Boolean> areNotificationsAllowed = new MutableLiveData<>(true);


    public HomeViewModel(@NonNull Application application) {
        super(application);
        heartRate.setValue(0);
        // Initialize simulator once
        heartbeatSimulator = new CSVHeartbeatSimulator();
        InputStream csvStream = getApplication().getResources().openRawResource(R.raw.heart_rate_clean);
        heartbeatSimulator.loadCSV(csvStream);
    }

    public LiveData<Integer> getHeartRate() {
        return heartRate;
    }

    public LiveData<Boolean> getShowBreakDialogEvent() {
        return showBreakDialogEvent;
    }

    // --- Timer Actions ---

    public boolean isTimerRunning() {
        return isTimerRunning;
    }

    public void toggleTimer() {
        if (isTimerRunning) {
            pauseTimer();
        } else {
            startTimer();
        }
    }

    private void startTimer() {
        if (isTimerRunning) return;

        isTimerRunning = true;
        lastStartTime = System.currentTimeMillis();

        // Start Simulator: restart=false means RESUME from where we left off
        heartbeatSimulator.startSimulation(this::onNewHeartRate, 5000, false);
    }

    public void pauseTimer() {
        if (!isTimerRunning) return;

        isTimerRunning = false;
        long now = System.currentTimeMillis();
        accumulatedTime += (now - lastStartTime);

        // Stop simulator but keep index (memory) for resume
        heartbeatSimulator.stopSimulation();
    }

    public void resetTimer() {
        pauseTimer(); // Stop everything first
        accumulatedTime = 0;
        lastStartTime = 0;
        heartRate.setValue(0);

        // Reset simulator to index 0
        heartbeatSimulator.reset();

        // Reset break logic
        lastBreakDismissTime = 0;
    }

    // Called by Fragment every second to update UI
    public long getCurrentTotalTime() {
        if (isTimerRunning) {
            return accumulatedTime + (System.currentTimeMillis() - lastStartTime);
        } else {
            return accumulatedTime;
        }
    }

    // --- Break Logic ---

    private void onNewHeartRate(int bpm) {
        heartRate.postValue(bpm);
        // We let the Fragment observe the BPM to count the "3 consecutive"
        // because that logic is simpler in the UI layer or here.
        // Let's keep the "3 count" logic where it was, but we handle the "Snooze" check here.

        //TODO: addins snnd ADDING HRV SETTINGS
        heartRateWindow.add(bpm);
        if (heartRateWindow.size() > HRV_WINDOW_SIZE) {
            heartRateWindow.remove(0);
        }
//         if (heartRateWindow.size() > 5) {
//             double rmssd = HeartRateVariability.computeRMSSD(heartRateWindow);
//             hrvLive.postValue(rmssd);
//         }
    }

    // Helper to check if we are allowed to show the dialog (5 min cooldown)
    public boolean canShowBreakDialog() {
        long now = System.currentTimeMillis();
        return (now - lastBreakDismissTime) > BREAK_SNOOZE_DURATION_MS;
    }

    public void onBreakDialogDismissed() {
        lastBreakDismissTime = System.currentTimeMillis();
    }

    public LiveData<Double> getHRV() {
        return  hrvLive;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (heartbeatSimulator != null) {
            heartbeatSimulator.stopSimulation();
        }
    }

    public LiveData<Boolean> getAreNotificationsAllowed() {
        return areNotificationsAllowed;
    }

    public void toggleNotificationPreference() {
        Boolean current = areNotificationsAllowed.getValue();
        if (current != null) {
            areNotificationsAllowed.setValue(!current);
        }
    }

    public void setNotificationsAllowed(boolean allowed) {
        areNotificationsAllowed.setValue(allowed);
    }


    public void calculateAndSaveBaseline(long userId) {
        new Thread(() -> {
            try {
                //Load all timestamps from the CSV
                InputStream is = getApplication().getResources().openRawResource(R.raw.heart_rate_clean);
                // open the CSV file
                InputStream is = getApplication().getResources().openRawResource(R.raw.heart_rate_clean);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                List<String> timestamps = new ArrayList<>();
                String line;
                reader.readLine(); // Skip header

                while ((line = reader.readLine()) != null) {
                    timestamps.add(line);
                }
                reader.close();

                if (timestamps.isEmpty()) {
                    Log.e("HomeViewModel", "No timestamps found in CSV");
                    return;
                }

                for (int i = 0; i < timestamps.size(); i++) {
                    Log.d("Timestamp:", timestamps.get(i));
                }

                //Use the HeartRateVariability class to compute BPM and HRV
                List<Float> bpmList = HeartRateVariability.computeBPM(timestamps);
                List<Float> hrvList = HeartRateVariability.computeHRV(timestamps);

                if (bpmList.isEmpty() || hrvList.isEmpty()) {
                    Log.e("HomeViewModel", "Failed to compute BPM or HRV");
                    return;
                }

                // Calculate averages
                float sumBpm = 0;
                for (float bpm : bpmList) sumBpm += bpm;
                float avgBpm = sumBpm / bpmList.size();

                float sumHrv = 0;
                for (float hrv : hrvList) sumHrv += hrv;
                float avgHrv = sumHrv / hrvList.size();

                Log.d("HomeViewModel", "Calculated baselines - HR: " + avgBpm + ", HRV: " + avgHrv);

                // Save to database
                AppDatabase db = AppDatabase.getInstance(getApplication());
                UserData user = db.userDataDao().getUserById(userId);
                if (user != null) {
                    user.setBaselineHr(avgBpm);
                    user.setBaselineHrv(avgHrv);
                    db.userDataDao().updateUser(user);

                    Log.d("HomeViewModel", "Baselines saved successfully");
                }

            } catch (Exception e) {
                Log.e("HomeViewModel", "Error calculating baselines", e);
            }
        }).start();
    }
}