package com.example.navigationbarstarter.ui.home;

import static com.example.navigationbarstarter.data.CSVHeartbeatSimulator.loadCsvTimestamp;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import androidx.appcompat.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.navigationbarstarter.R;
import com.example.navigationbarstarter.database.AppDatabase;
import com.example.navigationbarstarter.database.UserData;
import com.example.navigationbarstarter.database.session.SessionViewModel;
import com.example.navigationbarstarter.databinding.FragmentHomeBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.NotificationManager;
import android.content.Intent;
import android.provider.Settings;
import android.graphics.Color;
import android.content.res.ColorStateList;

public class HomeFragment extends Fragment implements SensorEventListener {

    private FragmentHomeBinding binding;
    private HomeViewModel homeViewModel;

    private SessionViewModel sessionViewModel;

    //UI Components
    private TextView focusModeStatus;
    private TextView timerText;
    private CircularProgressIndicator timerProgress;
    private ImageView heartIcon;
    private ImageView heartbeatLine;
    private View pulseCircleOuter;
    private View pulseCircleInner;

    private MaterialButton btnPlay;
    private MaterialButton btnStop;
    private MaterialButton btnReset;
    private MaterialButton btnTest;

    private MaterialButton btnNotification;

    // Logic Variables
    private int consecutiveHighBpmCount = 0;
    private boolean isBreakDialogShowing = false;
    private int currentHeartRate = 0;

    // To store user selection for the dialog
    // In a real app, you would fetch installed packages.
    // Here we use a common list for demonstration.
    final String[] appList = {"WhatsApp", "Instagram", "Facebook", "Telegram", "Gmail", "Messages", "TikTok"};
    final boolean[] checkedAppItems = new boolean[appList.length];

    // Database
    private ExecutorService executorService;
    private AppDatabase db;
    private long currentUserId = -1;

    // UI Update Loop
    private Handler uiHandler;
    private Runnable uiRunnable;

    private int sessionIndex;

    private List<List<String>> sessions;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        sessionViewModel = new ViewModelProvider(this).get(SessionViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        initializeViews();

        db = AppDatabase.getInstance(requireContext());
        executorService = Executors.newSingleThreadExecutor();
        loadUserId();

        //Used to return sessions from a CSV file
        sessionIndex = 0;

        //Initialize map
        sessions = loadCsvTimestamp(requireContext().getResources().openRawResource(R.raw.ten_sessions));

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

        updateNotificationIcon(true);

        btnNotification.setOnClickListener(v -> {
            NotificationManager nm = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
            // 1. Check Permission
            if (!nm.isNotificationPolicyAccessGranted()) {
                Toast.makeText(requireContext(), "Please grant Do Not Disturb permission", Toast.LENGTH_LONG).show();
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
            } else {
                // 2. Show the Dialog
                showNotificationSelectionDialog();
            }
        });

        // 2. Button Listeners

        // PLAY BUTTON (Start/Resume)
        btnPlay.setOnClickListener(v -> {
            if (!homeViewModel.isTimerRunning()) {
                homeViewModel.toggleTimer();
                updateUIState();

                // disable notifications (DND) if user selected apps
                setDoNotDisturb(true);
                updateNotificationIcon(false);
            }
        });

        // STOP BUTTON (Pause)
        btnStop.setOnClickListener(v -> {
            if (homeViewModel.isTimerRunning()) {
                homeViewModel.toggleTimer();
                updateUIState();

                // enable notifications
                setDoNotDisturb(false);
                updateNotificationIcon(true);
            }
        });

        // RESET BUTTON
        btnReset.setOnClickListener(v -> {
            homeViewModel.resetTimer();
            consecutiveHighBpmCount = 0;
            updateUIState();
            if(sessionIndex < sessions.size()) {
                List<String> sessionTS = this.sessions.get(sessionIndex);
                sessionViewModel.saveSession(currentUserId, sessionTS);
            }
            // enable notifications
            setDoNotDisturb(false);
            updateNotificationIcon(true);
        });

        btnTest.setOnClickListener(v -> showTestConfirmationDialog());

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

        // Observe save result once
        sessionViewModel.getSaveSessionResults().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                Toast.makeText(getContext(), "Session saved successfully", Toast.LENGTH_SHORT).show();
                // Increment sessionIndex after successful save
                sessionIndex++;
                // Optionally, reload sessions for charts
                sessionViewModel.loadComparedSessions(currentUserId);
            } else {
                Toast.makeText(getContext(), "Failed to save session", Toast.LENGTH_SHORT).show();
            }
        });

        return root;
    }

    private void showNotificationSelectionDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Which notifications do you want to disable?")
                .setMultiChoiceItems(appList, checkedAppItems, (dialog, which, isChecked) -> {
                    // Update the selection state
                    checkedAppItems[which] = isChecked;
                })
                .setPositiveButton("Confirm", (dialog, which) -> {
                    // Check if ANY app was selected
                    boolean anySelected = false;
                    for (boolean checked : checkedAppItems) {
                        if (checked) {
                            anySelected = true;
                            break;
                        }
                    }

                    // Logic: If any app is selected, we BLOCK notifications (allowed = false)
                    // If no apps are selected, we ALLOW notifications (allowed = true)
                    homeViewModel.setNotificationsAllowed(!anySelected);

                    if (anySelected) {
                        Toast.makeText(requireContext(), "Notifications blocked for selected apps.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "All notifications enabled.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showTestConfirmationDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Start Health Test")
                .setMessage("This test will last for about two minutes.\n\nDo you want to proceed?")
                .setNegativeButton("No", null)
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Start the second part of the flow
                    showMonitoringDialog();
                })
                .show();
    }

    private void showMonitoringDialog() {
        // 1. Inflate the custom layout
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.dialog_heart_test, null);

        // 2. Build the Dialog
        AlertDialog testDialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(false) // User must press Cancel button to exit
                .create();

        // make background transparent to see rounded corners if needed
        if (testDialog.getWindow() != null) {
            testDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        testDialog.show();

        // 3. Initialize Views inside the dialog
        ImageView ivHeartTest = dialogView.findViewById(R.id.ivHeartTest);
        View viewPulseTest = dialogView.findViewById(R.id.viewPulseTest);
        TextView tvTimer = dialogView.findViewById(R.id.tvTestTimer);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelTest);

        // 4. Start Animations for the Dialog
        ObjectAnimator pulseAnim = ObjectAnimator.ofFloat(viewPulseTest, "scaleX", 1f, 1.4f);
        pulseAnim.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnim.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnim.setDuration(1000);

        ObjectAnimator pulseAnimY = ObjectAnimator.ofFloat(viewPulseTest, "scaleY", 1f, 1.4f);
        pulseAnimY.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimY.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimY.setDuration(1000);

        ObjectAnimator heartBeat = ObjectAnimator.ofFloat(ivHeartTest, "scaleX", 1f, 1.2f);
        heartBeat.setRepeatCount(ValueAnimator.INFINITE);
        heartBeat.setRepeatMode(ValueAnimator.REVERSE);
        heartBeat.setDuration(800);

        ObjectAnimator heartBeatY = ObjectAnimator.ofFloat(ivHeartTest, "scaleY", 1f, 1.2f);
        heartBeatY.setRepeatCount(ValueAnimator.INFINITE);
        heartBeatY.setRepeatMode(ValueAnimator.REVERSE);
        heartBeatY.setDuration(800);

        pulseAnim.start();
        pulseAnimY.start();
        heartBeat.start();
        heartBeatY.start();

        // 5. Start a 2-minute Countdown
        CountDownTimer testTimer = new CountDownTimer(120000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = (millisUntilFinished / 1000) / 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                testDialog.dismiss();

                // Save baseline
                if (currentUserId != -1) {
                    homeViewModel.calculateAndSaveBaseline(currentUserId);
                }
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Test Complete")
                        .setMessage("The monitoring session is finished. Your baseline has been updated.")
                        .setPositiveButton("OK", null)
                        .show();
            }
        };
        testTimer.start();

        // 6. Handle Cancel Button
        btnCancel.setOnClickListener(v -> {
            testTimer.cancel();
            testDialog.dismiss();
        });

        testDialog.show();
    }

    private void updateUIState() {
        boolean isRunning = homeViewModel.isTimerRunning();
        long totalTime = homeViewModel.getCurrentTotalTime();

        // Update Status Text based on state
        if (isRunning) {
            focusModeStatus.setText("Focus Mode: Active");
            btnPlay.setAlpha(0.5f);
            btnPlay.setEnabled(false);
            btnStop.setAlpha(1.0f);
            btnStop.setEnabled(true);
        } else {
            if (totalTime > 0) {
                focusModeStatus.setText("Focus Mode: Paused");
            } else {
                focusModeStatus.setText("Focus Mode: Idle");
            }
            btnPlay.setAlpha(1.0f);
            btnPlay.setEnabled(true);
            btnStop.setAlpha(0.5f);
            btnStop.setEnabled(false);
        }

        // Timer Text with BPM
        long seconds = (totalTime / 1000) % 60;
        long minutes = (totalTime / 1000) / 60;
        String timeString = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);

        String bpmString = "";
        if (isRunning && currentHeartRate > 0) {
            bpmString = String.format(Locale.getDefault(), " | %d BPM", currentHeartRate);
        }

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

        btnPlay = binding.btnPlay;
        btnStop = binding.btnStop;
        btnReset = binding.btnReset;
        btnTest = binding.btnTest;
        btnNotification = binding.btnNotification;
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

    private void updateNotificationIcon(boolean allowed) {
        if (allowed) {
            // Notifications ALLOWED (Default)
            btnNotification.setIconResource(R.drawable.ic_notifications_black_24dp);
            // Set standard color (Black)
            btnNotification.setIconTint(ColorStateList.valueOf(requireContext().getColor(R.color.text_primary)));
            btnNotification.setStrokeColor(ColorStateList.valueOf(requireContext().getColor(R.color.text_primary)));
        } else {
            // Notifications BLOCKED (Red Oblique Band)
            btnNotification.setIconResource(R.drawable.ic_notifications_disable);
            // Set RED color
            btnNotification.setIconTint(ColorStateList.valueOf(Color.RED));
            btnNotification.setStrokeColor(ColorStateList.valueOf(Color.RED));
        }
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

    private void setDoNotDisturb(boolean enableDnd) {
        NotificationManager nm = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (nm != null && nm.isNotificationPolicyAccessGranted()) {
            Boolean areNotificationsAllowed = homeViewModel.getAreNotificationsAllowed().getValue();

            // Block ONLY if user explicitly disabled notifications via the dialog
            boolean shouldBlock = (areNotificationsAllowed != null && !areNotificationsAllowed);

            if (enableDnd && shouldBlock) {
                // Focus Mode START -> Silence Phone
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
            } else {
                // Focus Mode STOP -> Restore Sounds
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
            }
        }
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