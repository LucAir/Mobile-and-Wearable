package com.example.navigationbarstarter.ui.access;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.navigationbarstarter.R;
import com.example.navigationbarstarter.database.AppDatabase;
import com.example.navigationbarstarter.database.UserData;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilUsername, tilAge, tilEmail, tilPassword, tilConfirmPassword;
    private TextInputEditText etUsername, etAge, etEmail, etPassword, etConfirmPassword;

    //Triggers registration
    private Button btnRegister;

    //Go back to login
    private TextView tvGoToLogin;

    //Shows loading
    private ProgressBar progressBar;
    private AppDatabase appDatabase;

    //Avoid the user to spam click on registration and create multiple equal registration
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

    //Run task in background
    private ExecutorService executorService;

    //Post results back on the main UI thread
    private Handler mainHandler;

    //Allow to cancel the background task if user leaves early
    private Future<?> currentTask;

    //Debounce handlers for async validation
    private Handler validationHandler;
    private Runnable usernameValidationRunnable;
    private Runnable emailValidationRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        //Show the arrow to go back
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        //Initialize threading components FIRST
        executorService = Executors.newCachedThreadPool();
        mainHandler = new Handler(Looper.getMainLooper());
        validationHandler = new Handler(Looper.getMainLooper());
        appDatabase = AppDatabase.getInstance(this);

        //Set up view
        initializeViews();

        //Set up listeners
        setupListeners();

        //Setup real-time validation
        setupRealtimeValidation();

        //Handle modern back gesture
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!isProcessing.get()) {
                    setEnabled(false); //disable this callback
                    getOnBackPressedDispatcher().onBackPressed(); //perform default behavior
                } else {
                    showToast("Please wait for registration to complete");
                }
            }
        });
    }

    //Initialize elements
    private void initializeViews() {
        tilUsername = findViewById(R.id.tilRegisterUsername);
        tilAge = findViewById(R.id.tilRegisterAge);
        tilEmail = findViewById(R.id.tilRegisterEmail);
        tilPassword = findViewById(R.id.tilRegisterPassword);
        tilConfirmPassword = findViewById(R.id.tilRegisterConfirmPassword);

        etUsername = findViewById(R.id.etRegisterUsername);
        etAge = findViewById(R.id.etRegisterAge);
        etEmail = findViewById(R.id.etRegisterEmail);
        etPassword = findViewById(R.id.etRegisterPassword);
        etConfirmPassword = findViewById(R.id.etRegisterConfirmPassword);

        btnRegister = findViewById(R.id.btnRegister);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);
        progressBar = findViewById(R.id.progressBar);

        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    //Set up listener
    private void setupListeners() {
        //When click run register method (do controls on fields ecc...)
        btnRegister.setOnClickListener(v -> register());
        //When click close this screen
        tvGoToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    //Validate fields as user types not only when pressed is clicked
    private void setupRealtimeValidation() {
        //Username validation (with debounce - wait 500ms after user stops typing)
        etUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilUsername.setError(null); //Clear error while typing
            }

            @Override
            public void afterTextChanged(Editable s) {
                String username = s.toString().trim();

                //Remove previous validation task
                if (usernameValidationRunnable != null) {
                    validationHandler.removeCallbacks(usernameValidationRunnable);
                }

                //Basic validation first
                if (username.isEmpty()) {
                    return;
                }

                if (username.length() < 3) {
                    tilUsername.setError("Username must be at least 3 characters");
                    return;
                }

                if (username.length() > 20) {
                    tilUsername.setError("Username must be less than 20 characters");
                    return;
                }

                if (!username.matches("^[a-zA-Z0-9_]+$")) {
                    tilUsername.setError("Only letters, numbers, and underscores allowed");
                    return;
                }

                //Check if username exists in database (debounced)
                usernameValidationRunnable = () -> checkUsernameExists(username);
                validationHandler.postDelayed(usernameValidationRunnable, 500);
            }
        });

        //Email validation (with debounce)
        etEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilEmail.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
                String email = s.toString().trim();

                if (emailValidationRunnable != null) {
                    validationHandler.removeCallbacks(emailValidationRunnable);
                }

                if (email.isEmpty()) {
                    return;
                }

                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    tilEmail.setError("Invalid email format");
                    return;
                }

                //Check if email exists in database
                emailValidationRunnable = () -> checkEmailExists(email);
                validationHandler.postDelayed(emailValidationRunnable, 500);
            }
        });

        //Password validation
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilPassword.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
                String password = s.toString();
                if (!password.isEmpty() && password.length() < 6) {
                    tilPassword.setError("Password must be at least 6 characters");
                } else if (!password.isEmpty() && password.length() > 50) {
                    tilPassword.setError("Password must be less than 50 characters");
                }

                //Also check confirm password match
                String confirmPassword = etConfirmPassword.getText().toString();
                if (!confirmPassword.isEmpty() && !password.equals(confirmPassword)) {
                    tilConfirmPassword.setError("Passwords do not match");
                } else if (!confirmPassword.isEmpty()) {
                    tilConfirmPassword.setError(null);
                }
            }
        });

        //Confirm password validation
        etConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilConfirmPassword.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
                String confirmPassword = s.toString();
                String password = etPassword.getText().toString();

                if (!confirmPassword.isEmpty() && !password.equals(confirmPassword)) {
                    tilConfirmPassword.setError("Passwords do not match");
                }
            }
        });

        //Age validation
        etAge.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilAge.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
                String ageStr = s.toString().trim();
                if (!ageStr.isEmpty()) {
                    try {
                        int age = Integer.parseInt(ageStr);
                        if (age < 13) {
                            tilAge.setError("You must be at least 13 years old");
                        } else if (age > 120) {
                            tilAge.setError("Invalid age");
                        }
                    } catch (NumberFormatException e) {
                        tilAge.setError("Invalid number");
                    }
                }
            }
        });
    }

    private void checkUsernameExists(String username) {
        executorService.execute(() -> {
            try {
                UserData existingUser = appDatabase.userDataDao().getUserByUsername(username);

                if (!isFinishing() && !isDestroyed()) {
                    mainHandler.post(() -> {
                        // Only show error if the field still has the same value
                        String currentUsername;
                        if (etUsername.getText() != null) {
                            currentUsername = etUsername.getText().toString().trim();
                        } else {
                            return;
                        }
                        if (currentUsername.equals(username)) {
                            if (existingUser != null) {
                                tilUsername.setError("Username already taken");
                            } else {
                                tilUsername.setError(null);
                            }
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void checkEmailExists(String email) {
        executorService.execute(() -> {
            try {
                UserData existingEmail = appDatabase.userDataDao().getUserByEmail(email);

                if (!isFinishing() && !isDestroyed()) {
                    mainHandler.post(() -> {
                        String currentEmail = etEmail.getText().toString().trim();
                        if (currentEmail.equals(email)) {
                            if (existingEmail != null) {
                                tilEmail.setError("Email already registered");
                            } else {
                                tilEmail.setError(null);
                            }
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void register() {
        //Prevent double-click -> concurrent registration attempt
        if(!isProcessing.compareAndSet(false, true)) {
            showToast("Please wait, processing...");
            return;
        }

        //Get values safely
        String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";
        String ageText = etAge.getText() != null ? etAge.getText().toString().trim() : "";

        //Clear all previous errors
        clearAllErrors();

        //Validate and show errors in TextInputLayout
        boolean hasErrors = false;

        if (username.isEmpty()) {
            tilUsername.setError("Username is required");
            hasErrors = true;
        } else if (username.length() < 3) {
            tilUsername.setError("Username must be at least 3 characters");
            hasErrors = true;
        } else if (username.length() > 20) {
            tilUsername.setError("Username must be less than 20 characters");
            hasErrors = true;
        } else if (!username.matches("^[a-zA-Z0-9_]+$")) {
            tilUsername.setError("Only letters, numbers, and underscores allowed");
            hasErrors = true;
        }

        if (email.isEmpty()) {
            tilEmail.setError("Email is required");
            hasErrors = true;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Invalid email format");
            hasErrors = true;
        }

        //Regex: at least 8 chars, one uppercase, one special character
        Pattern pattern = Pattern.compile("^(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{8,}$");
        Matcher matcher = pattern.matcher(password);

        if (password.isEmpty()) {
            tilPassword.setError("Password is required");
            hasErrors = true;
        } else if (!matcher.matches()) {
            tilPassword.setError("Password must include at least one uppercase letter and one special character");
            hasErrors = true;
        } else if (password.length() > 50) {
            tilPassword.setError("Password must be less than 50 characters");
            hasErrors = true;
        } else if (password.length() < 8) {
            tilPassword.setError("Password must be at least 8 characters");
            hasErrors = true;
        } else {
            tilPassword.setError(null); //clear error
        }



        //confirm password checks
        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.setError("Please confirm your password");
            hasErrors = true;
        } else if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError("Passwords do not match");
            hasErrors = true;
        } else {
            tilConfirmPassword.setError(null);
        }

        //Parse age - use final array to avoid lambda issue
        final int[] ageArray = new int[1];
        if (ageText.isEmpty()) {
            tilAge.setError("Age is required");
            hasErrors = true;
        } else {
            try {
                ageArray[0] = Integer.parseInt(ageText);
                if (ageArray[0] < 13) {
                    tilAge.setError("You must be at least 13 years old");
                    hasErrors = true;
                } else if (ageArray[0] > 120) {
                    tilAge.setError("Invalid age");
                    hasErrors = true;
                }
            } catch (NumberFormatException e) {
                tilAge.setError("Invalid number");
                hasErrors = true;
            }
        }

        if (hasErrors) {
            isProcessing.set(false);
            return;
        }

        final int age = ageArray[0]; //Extract from array for use in lambda

        //Show loading state
        setLoadingState(true);

        //Execute registration on background thread
        currentTask = executorService.submit(() -> {
            try {
                Thread.sleep(300);

                //Check if a user is already in the db
                UserData existUser = appDatabase.userDataDao().getUserByUsername(username);
                if(existUser != null) {
                    if(!isFinishing() && !isDestroyed()) {
                        mainHandler.post(() -> {
                            tilUsername.setError("Username already taken");
                            setLoadingState(false);
                            isProcessing.set(false);
                        });
                    }
                    return;
                }

                //Check for existing email
                UserData existEmail = appDatabase.userDataDao().getUserByEmail(email);
                if(existEmail != null) {
                    if(!isFinishing() && !isDestroyed()) {
                        mainHandler.post(() -> {
                            tilEmail.setError("Email already registered");
                            setLoadingState(false);
                            isProcessing.set(false);
                        });
                    }
                    return;
                }

                //Create new user with all fields
                UserData newUser = new UserData(username, age, email, password);
                long resultOperation = appDatabase.userDataDao().insert(newUser);
                //TODO: understand how to log
                Log.e("NEW USER", "msg: "+ resultOperation);

                //Check if activity is still alive
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                //Registration successful
                mainHandler.post(() -> handleRegistrationSuccess(resultOperation));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                mainHandler.post(() -> {
                    showToast("Registration cancelled");
                    setLoadingState(false);
                    isProcessing.set(false);
                });
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    showToast("Registration error: " + e.getMessage());
                    setLoadingState(false);
                    isProcessing.set(false);
                });
            }
        });
    }

    private void clearAllErrors() {
        if (tilUsername != null) tilUsername.setError(null);
        if (tilAge != null) tilAge.setError(null);
        if (tilEmail != null) tilEmail.setError(null);
        if (tilPassword != null) tilPassword.setError(null);
        if (tilConfirmPassword != null) tilConfirmPassword.setError(null);
    }

    private void handleRegistrationSuccess(long userId) {
        try {
            if (userId > 0) {
                showToast("Registration successful! Please login");
                clearFields();

                //Navigate to login
                mainHandler.postDelayed(() -> {
                    if(!isFinishing() && !isDestroyed()) {
                        finish();
                    }
                }, 1000);
            } else {
                showToast("Registration failed. Please try again");
                setLoadingState(false);
                isProcessing.set(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showToast("Error processing registration");
            setLoadingState(false);
            isProcessing.set(false);
        }
    }

    private void clearFields() {
        if (etUsername != null) etUsername.setText("");
        if (etAge != null) etAge.setText("");
        if (etEmail != null) etEmail.setText("");
        if (etPassword != null) etPassword.setText("");
        if (etConfirmPassword != null) etConfirmPassword.setText("");
    }

    private void setLoadingState(boolean isLoading) {
        if (btnRegister != null) {
            btnRegister.setEnabled(!isLoading);
            btnRegister.setText(isLoading ? "Creating Account..." : "Register");
        }

        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }

        if (etUsername != null) etUsername.setEnabled(!isLoading);
        if (etAge != null) etAge.setEnabled(!isLoading);
        if (etEmail != null) etEmail.setEnabled(!isLoading);
        if (etPassword != null) etPassword.setEnabled(!isLoading);
        if (etConfirmPassword != null) etConfirmPassword.setEnabled(!isLoading);
        if (tvGoToLogin != null) tvGoToLogin.setEnabled(!isLoading);
    }

    private void showToast(String message) {
        if(!isFinishing() && !isDestroyed()) {
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

        //Cancel ongoing task
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

        //Remove all pending callbacks
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }

        //Remove validation callbacks
        if (validationHandler != null) {
            validationHandler.removeCallbacksAndMessages(null);
        }

        //Reset state
        isProcessing.set(false);
    }
}
