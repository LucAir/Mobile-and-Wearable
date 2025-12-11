package com.example.navigationbarstarter.ui.home;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.navigationbarstarter.database.AppDatabase;
import com.example.navigationbarstarter.database.UserData;
import com.example.navigationbarstarter.databinding.FragmentHomeBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment implements SensorEventListener {

    private FragmentHomeBinding binding;
    private HomeViewModel homeViewModel;

    // UI Components
    private TextView focusModeStatus;
    private TextView timerText;
    private CircularProgressIndicator timerProgress;
    private ImageView heartIcon;
    private ImageView heartbeatLine;
    private View pulseCircleOuter;
    private View pulseCircleInner;

    // New Buttons
    private MaterialButton btnPlay;
    private MaterialButton btnStop;
    private MaterialButton btnReset;

    // Logic Variables
    private int consecutiveHighBpmCount = 0;
    private boolean isBreakDialogShowing = false;
    private int currentHeartRate = 0;

    // Database
    private ExecutorService executorService;
    private AppDatabase db;
    private long currentUserId = -1;

    // UI Update Loop
    private Handler uiHandler;
    private Runnable uiRunnable;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        initializeViews();

        db = AppDatabase.getInstance(requireContext());
        executorService = Executors.newSingleThreadExecutor();
        loadUserId();

        // 1. Setup UI Update Loop
        uiHandler = new Handler(Looper.getMainLooper());
        uiRunnable = new Runnable() {
            @Override
            public void run() {
                updateUIState();
                uiHandler.postDelayed(this, 1000);
            }
        };
        uiHandler.post(uiRunnable);

        // 2. Button Listeners

        // PLAY BUTTON (Start/Resume)
        btnPlay.setOnClickListener(v -> {
            if (!homeViewModel.isTimerRunning()) {
                homeViewModel.toggleTimer(); // Starts timer
                updateUIState();
            }
        });

        // STOP BUTTON (Pause)
        btnStop.setOnClickListener(v -> {
            if (homeViewModel.isTimerRunning()) {
                homeViewModel.toggleTimer(); // Pauses timer
                updateUIState();
            }
        });

        // RESET BUTTON
        btnReset.setOnClickListener(v -> {
            homeViewModel.resetTimer();
            consecutiveHighBpmCount = 0;
            updateUIState();
        });

        // 3. Observe Heart Rate
        homeViewModel.getHeartRate().observe(getViewLifecycleOwner(), bpm -> {
            currentHeartRate = bpm;
            updateHeartRateUI();

            if (homeViewModel.isTimerRunning()) {
                checkMovementLogic(bpm);
            }
        });

        startHeartbeatAnimation();
        startPulseAnimation();

        return root;
    }

    private void updateUIState() {
        boolean isRunning = homeViewModel.isTimerRunning();
        long totalTime = homeViewModel.getCurrentTotalTime();

        // Update Status Text based on state
        if (isRunning) {
            focusModeStatus.setText("Focus Mode: Active");
            // Optional: visually disable Play button to show it's active
            btnPlay.setAlpha(0.5f);
            btnStop.setAlpha(1.0f);
        } else {
            if (totalTime > 0) {
                focusModeStatus.setText("Focus Mode: Paused");
            } else {
                focusModeStatus.setText("Focus Mode: Idle");
            }
            // Optional: visually enable Play button
            btnPlay.setAlpha(1.0f);
            btnStop.setAlpha(0.5f);
        }

        // Update Timer Text
        long seconds = (totalTime / 1000) % 60;
        long minutes = (totalTime / 1000) / 60;
        String timeString = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);

        // Show BPM next to timer only if running (or always if you prefer)
        String bpmString = isRunning ? String.format(Locale.getDefault(), "\n%d BPM", currentHeartRate) : "";

        // Note: Layout constraints might need checking if BPM adds a new line,
        // strictly following your design, we might just keep the time:
        timerText.setText(timeString);

        // Update Progress
        int progressPercentage = (int) ((totalTime / 1000) % 1500) / 15;
        timerProgress.setProgress(progressPercentage);

        checkAndAwardTokens(totalTime);
    }

    private void checkMovementLogic(int bpm) {
        if (bpm >= 80) {
            consecutiveHighBpmCount++;
        } else {
            consecutiveHighBpmCount = 0;
        }

        if (consecutiveHighBpmCount >= 3) {
            consecutiveHighBpmCount = 0;
            if (homeViewModel.canShowBreakDialog()) {
                showBreakPopup();
            }
        }
    }

    private void showBreakPopup() {
        if (isBreakDialogShowing) return;
        isBreakDialogShowing = true;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Movement Detected")
                .setMessage("Movement detected, need a break?")
                .setCancelable(false)
                .setPositiveButton("Yes", (dialog, which) -> {
                    if(homeViewModel.isTimerRunning()) {
                        homeViewModel.toggleTimer(); // Pause
                    }
                    focusModeStatus.setText("Focus Mode: Taking a Break");
                    isBreakDialogShowing = false;
                    updateUIState();
                })
                .setNegativeButton("No", (dialog, which) -> {
                    homeViewModel.onBreakDialogDismissed();
                    isBreakDialogShowing = false;
                })
                .show();
    }

    private void initializeViews() {
        focusModeStatus = binding.focusModeStatus;
        timerText = binding.timerText;
        timerProgress = binding.timerProgress;
        heartIcon = binding.heartIcon;
        heartbeatLine = binding.heartbeatLine;
        pulseCircleOuter = binding.pulseCircleOuter;
        pulseCircleInner = binding.pulseCircleInner;

        // New Buttons Mapping
        btnPlay = binding.btnPlay;
        btnStop = binding.btnStop;
        btnReset = binding.btnReset;
    }

    private void updateHeartRateUI() {
        adjustHeartbeatSpeed(currentHeartRate);
    }

    // --- Token Logic ---
    private void checkAndAwardTokens(long elapsedMillis) {
        if (currentUserId == -1) return;
        long totalMinutesPassed = elapsedMillis / 60000;
        SharedPreferences prefs = requireContext().getSharedPreferences("TimerPrefs", Context.MODE_PRIVATE);
        long minutesAwarded = prefs.getLong("minutesAwarded", 0);

        if (totalMinutesPassed > minutesAwarded) {
            long tokensToAdd = totalMinutesPassed - minutesAwarded;
            addTokensToUser(tokensToAdd);
            prefs.edit().putLong("minutesAwarded", totalMinutesPassed).apply();
        }
    }

    private void addTokensToUser(long amount) {
        executorService.execute(() -> {
            UserData user = db.userDataDao().getUserById(currentUserId);
            if (user != null) {
                user.setToken(user.getToken() + amount);
                db.userDataDao().updateUser(user);
            }
        });
    }

    private void loadUserId() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        currentUserId = sharedPreferences.getLong("userId", -1);
    }

    // --- Animations ---
    private ObjectAnimator scaleX, scaleY, alpha;

    private void startHeartbeatAnimation() {
        scaleX = ObjectAnimator.ofFloat(heartIcon, "scaleX", 1.0f, 1.15f, 1.0f);
        scaleY = ObjectAnimator.ofFloat(heartIcon, "scaleY", 1.0f, 1.15f, 1.0f);
        alpha = ObjectAnimator.ofFloat(heartbeatLine, "alpha", 0.6f, 1.0f, 0.6f);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        alpha.setRepeatCount(ValueAnimator.INFINITE);
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleX.start();
        scaleY.start();
        alpha.start();
    }

    private void adjustHeartbeatSpeed(int bpm) {
        long beatDuration = bpm > 0 ? 60000 / bpm : 800;
        if (scaleX != null) {
            scaleX.setDuration(beatDuration);
            scaleY.setDuration(beatDuration);
            alpha.setDuration(beatDuration);
        }
    }

    private void startPulseAnimation() {
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
    public void onSensorChanged(SensorEvent event) { }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (uiHandler != null) uiHandler.removeCallbacks(uiRunnable);
        if (executorService != null) executorService.shutdown();
        binding = null;
    }
}