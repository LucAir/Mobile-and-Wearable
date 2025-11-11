package com.example.navigationbarstarter.ui.home;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.navigationbarstarter.databinding.FragmentHomeBinding;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.google.android.material.slider.Slider;


public class HomeFragment extends Fragment implements SensorEventListener {

    private FragmentHomeBinding binding;
    private HomeViewModel homeViewModel;


    // UI Components
    private TextView focusModeStatus;
    private TextView timerText;
    private TextView timerSubtitle;
    private TextView heartRateText;
    private CircularProgressIndicator timerProgress;
    private ImageView heartIcon;
    private ImageView heartbeatLine;
    private View pulseCircleOuter;
    private View pulseCircleInner;

    // Sensor Management
    private SensorManager sensorManager;
    private Sensor heartRateSensor;

    // Mode tracking
    private static final String MODE_FOCUS = "Focus Mode: Active";
    private static final String MODE_BREAK = "Break Mode: Active";
    private static final String MODE_REST = "Rest Mode: Active";

    private String currentMode = MODE_FOCUS;
    private long modeStartTime;
    private int currentHeartRate = 72; // Default

    // Timer
    private Handler timerHandler;
    private Runnable timerRunnable;
    private long elapsedTimeInMode = 0;

    // Executor for database operations
    private ExecutorService executorService;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize UI components
        initializeViews();

        // Initialize sensors
        initializeSensors();

        // Initialize timer
        initializeTimer();

        // Initialize database executor
        executorService = Executors.newSingleThreadExecutor();

        // Start animations
        startHeartbeatAnimation();
        startPulseAnimation();

        // Set initial mode
        modeStartTime = System.currentTimeMillis();
        updateModeDisplay();

        return root;
    }

    private Slider heartRateSlider;

    private void initializeViews() {
        focusModeStatus = binding.focusModeStatus;
        timerText = binding.timerText;
        timerSubtitle = binding.timerSubtitle;
        heartRateText = binding.heartRateText;
        timerProgress = binding.timerProgress;
        heartIcon = binding.heartIcon;
        heartbeatLine = binding.heartbeatLine;
        pulseCircleOuter = binding.pulseCircleOuter;
        pulseCircleInner = binding.pulseCircleInner;
        heartRateSlider = binding.heartRateSlider; // new

        // Show heart rate text for debugging
        heartRateText.setVisibility(View.VISIBLE);

        // Setup slider listener
        setupHeartRateSlider();
    }


    private void setupHeartRateSlider() {
        heartRateSlider.addOnChangeListener((slider, value, fromUser) -> {
            currentHeartRate = Math.round(value);
            heartRateText.setText(String.format(Locale.getDefault(), "%d BPM", currentHeartRate));
            adjustHeartbeatSpeed(currentHeartRate);
            updateModeBasedOnHeartRate();
        });
    }


    private void initializeSensors() {
        sensorManager = (SensorManager) requireActivity().getSystemService(android.content.Context.SENSOR_SERVICE);
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

        if (heartRateSensor != null) {
            sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void initializeTimer() {
        timerHandler = new Handler(Looper.getMainLooper());
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                elapsedTimeInMode = System.currentTimeMillis() - modeStartTime;
                updateTimerDisplay();
                updateModeBasedOnHeartRate();
                timerHandler.postDelayed(this, 1000); // Update every second
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void updateTimerDisplay() {
        long seconds = (elapsedTimeInMode / 1000) % 60;
        long minutes = (elapsedTimeInMode / 1000) / 60;

        String timeString = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        timerText.setText(timeString);

        // Update progress indicator (example: 25 minute cycle)
        int progressPercentage = (int) ((elapsedTimeInMode / 1000) % 1500) / 15; // 25 min = 1500 sec
        timerProgress.setProgress(progressPercentage);

        // Update subtitle based on mode
        updateSubtitle();
    }

    private void updateSubtitle() {
        switch (currentMode) {
            case MODE_FOCUS:
                timerSubtitle.setText("/ focused");
                break;
            case MODE_BREAK:
                timerSubtitle.setText("/ 5 min break");
                break;
            case MODE_REST:
                timerSubtitle.setText("/ resting");
                break;
        }
    }

    private void updateModeBasedOnHeartRate() {
        // Define heart rate thresholds for different modes
        // Focus: 60-80 BPM (calm, concentrated)
        // Break: 80-100 BPM (slightly elevated, moving around)
        // Rest: < 60 BPM (very relaxed)

        String newMode;
        if (currentHeartRate < 60) {
            newMode = MODE_REST;
        } else if (currentHeartRate >= 60 && currentHeartRate <= 80) {
            newMode = MODE_FOCUS;
        } else {
            newMode = MODE_BREAK;
        }

        // If mode changed, reset timer
        if (!newMode.equals(currentMode)) {
            currentMode = newMode;
            modeStartTime = System.currentTimeMillis();
            elapsedTimeInMode = 0;
            updateModeDisplay();

            // Save mode change to database
            saveModeChangeToDatabase();
        }
    }

    private void updateModeDisplay() {
        focusModeStatus.setText(currentMode);

        // You can also change colors/themes based on mode here
        // For example, different gradients for different modes
    }

    private void saveModeChangeToDatabase() {
        executorService.execute(() -> {
            // TODO: Implement database save logic
            // Example:
            // AppDatabase db = AppDatabase.getInstance(requireContext());
            // ModeChange modeChange = new ModeChange(currentMode, modeStartTime, currentHeartRate);
            // db.modeChangeDao().insert(modeChange);
        });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            currentHeartRate = (int) event.values[0];

            // Update heart rate display
            if (heartRateText.getVisibility() == View.VISIBLE) {
                heartRateText.setText(String.format(Locale.getDefault(), "%d BPM", currentHeartRate));
            }

            // Adjust heartbeat animation speed based on heart rate
            adjustHeartbeatSpeed(currentHeartRate);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle accuracy changes if needed
    }

    private void startHeartbeatAnimation() {
        // Animate heart icon with beat
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(heartIcon, "scaleX", 1.0f, 1.15f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(heartIcon, "scaleY", 1.0f, 1.15f, 1.0f);

        scaleX.setDuration(800);
        scaleY.setDuration(800);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());

        scaleX.start();
        scaleY.start();

        // Animate heartbeat line
        ObjectAnimator alpha = ObjectAnimator.ofFloat(heartbeatLine, "alpha", 0.6f, 1.0f, 0.6f);
        alpha.setDuration(800);
        alpha.setRepeatCount(ValueAnimator.INFINITE);
        alpha.start();
    }

    private void adjustHeartbeatSpeed(int bpm) {
        // Calculate beat duration in milliseconds (60000 ms / bpm)
        long beatDuration = bpm > 0 ? 60000 / bpm : 800;

        // You can restart animations with new duration here if needed
    }

    private void startPulseAnimation() {
        // Animate pulse circles
        ObjectAnimator pulseOuter = ObjectAnimator.ofFloat(pulseCircleOuter, "alpha", 0.3f, 0.6f, 0.3f);
        pulseOuter.setDuration(2000);
        pulseOuter.setRepeatCount(ValueAnimator.INFINITE);
        pulseOuter.start();

        ObjectAnimator pulseInner = ObjectAnimator.ofFloat(pulseCircleInner, "alpha", 0.4f, 0.8f, 0.4f);
        pulseInner.setDuration(2000);
        pulseInner.setStartDelay(500);
        pulseInner.setRepeatCount(ValueAnimator.INFINITE);
        pulseInner.start();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (heartRateSensor != null) {
            sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (timerHandler != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
        if (executorService != null) {
            executorService.shutdown();
        }
        binding = null;
    }
}