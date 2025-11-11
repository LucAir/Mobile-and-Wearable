package com.example.navigationbarstarter.ui.access;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.navigationbarstarter.MainActivity;
import com.example.navigationbarstarter.R;
import com.example.navigationbarstarter.database.AppDatabase;
import com.example.navigationbarstarter.database.UserData;
import com.google.android.material.textfield.TextInputEditText;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etUsername, etPassword;
    private Button btnLogin;
    private TextView tvGoToRegister;
    private ProgressBar progressBar;
    private AppDatabase appDatabase;
    private ExecutorService executorService;
    private Handler mainHandler;
    private AtomicBoolean isProcessing = new AtomicBoolean();
    private Future<?> currentTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //Show the arrow to go back
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        //Initialize threading components first
        executorService = Executors.newCachedThreadPool();
        mainHandler = new Handler(Looper.getMainLooper());
        appDatabase = AppDatabase.getInstance(this);

        //Check if user is already logged in (on background thread)
        checkExistingSession();

        //Set up view
        initializeViews();
        setUpListeners();
    }

    private void initializeViews() {
        etUsername = findViewById(R.id.etLoginUsername);
        etPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);
        progressBar = findViewById(R.id.progressBar);

        if (progressBar != null) {
            progressBar.setVisibility(TextView.GONE);
        }
    }

    private void setUpListeners(){
        btnLogin.setOnClickListener(v -> login());
        tvGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void checkExistingSession() {
        executorService.execute(() -> {
            try {
                SharedPreferences preferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                boolean isLoggedIn = preferences.getBoolean("isLoggedIn", false);
                int userId = preferences.getInt("userId", -1);

                if (isLoggedIn && userId != -1) {
                    //Verify user still exists in the database
                    UserData user = appDatabase.userDataDao().getUserById(userId);

                    if (!isFinishing() && !isDestroyed()) {
                        if(user != null) {
                            mainHandler.post(this::navigateToMainActivity);
                        } else {
                            mainHandler.post(() -> {
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.clear();
                                editor.apply();
                            });
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void login() {
        //Prevent concurrent login attempts
        if(!isProcessing.compareAndSet(false, true)) {
            showToast("Please wait, processing...");
            return;
        }

        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        //Input validation
        if (username.isEmpty() || password.isEmpty()) {
            showToast("Please fill all fields");
            isProcessing.set(false);
            return;
        }

        //Show loading state
        setLoadingState(true);

        //Execute login on background thread
        currentTask = executorService.submit(() -> {
            try {
                Thread.sleep(300);

                UserData user = appDatabase.userDataDao().login(username, password);

                //Check if activity is still alive before updating UI
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                mainHandler.post(() -> handleLoginResult(user));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                mainHandler.post(() -> {
                    showToast("Login cancelled");
                    setLoadingState(false);
                    isProcessing.set(false);
                });
            }
        });
    }

    private void handleLoginResult(UserData user) {
        try {
            if (user != null) {
                //Save login session
                SharedPreferences preferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("isLoggedIn", true);
                editor.putInt("userId", user.getId());
                editor.putString("username", user.getUsername());
                editor.putLong("loginTime", System.currentTimeMillis());
                editor.apply();
                showToast("Welcome back, " + user.getUsername());
                navigateToMainActivity();
            } else {
                showToast("Invalid username or password");
                setLoadingState(false);
                isProcessing.set(false);

                //Clear password field on failed login
                if (etPassword != null) {
                    etPassword.setText("");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showToast("Error processing login");
            setLoadingState(false);
            isProcessing.set(false);
        }
    }

    private void setLoadingState(boolean isLoading) {
        if (btnLogin != null) {
            btnLogin.setEnabled(!isLoading);
            btnLogin.setText(isLoading ? "logging in..." : "Login");
        }

        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }

        if (etUsername != null) etUsername.setEnabled(!isLoading);
        if (etPassword != null) etPassword.setEnabled(!isLoading);
        if (tvGoToRegister != null) etPassword.setEnabled(!isLoading);
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showToast(String message) {
        if (!isFinishing() && !isDestroyed()) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish(); //go back to AccessActivity
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Cancel ongoing tasks when activity is paused
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //Clean up threading resources
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }

        //Remove all pending callbacks to prevent memory leaks
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }

        //Reset state
        isProcessing.set(false);
    }
}
