package com.example.navigationbarstarter.ui.notifications;

import static android.content.Context.MODE_PRIVATE;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.navigationbarstarter.database.AppDatabase;
import com.example.navigationbarstarter.database.UserData;
import com.example.navigationbarstarter.databinding.FragmentNotificationsBinding;
import com.example.navigationbarstarter.databinding.FragmentSettingsBinding;
import com.example.navigationbarstarter.ui.access.LoginActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class SettingsFragment extends Fragment {

    private TextInputLayout tilName, tilSurname, tilCurrentPassword, tilNewPassword, tilConfirmNewPassword;
    private TextInputEditText etUsername, etName, etSurname, etEmail, etAge;
    private TextInputEditText etCurrentPassword, etNewPassword, etConfirmNewPassword;
    private Button btnSaveProfile, btnChangePassword, btnLogout, btnDeleteAccount;
    private ProgressBar progressBar;
    private AppDatabase database;
    private ExecutorService executorService;
    private Handler mainHandler;
    private AtomicBoolean isProcessing = new AtomicBoolean(false);
    private Future<?> currentTask;
    private int currentUserId;
    private UserData currentUser;

    private FragmentSettingsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        NotificationsViewModel notificationsViewModel =
                new ViewModelProvider(this).get(NotificationsViewModel.class);

        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        //Initialize threading
        executorService = Executors.newCachedThreadPool();
        mainHandler = new Handler(Looper.getMainLooper());
        database = AppDatabase.getInstance(requireContext());

        //Get current user ID from shared preferences
        SharedPreferences preferences = getActivity().getSharedPreferences("UserPrefs", MODE_PRIVATE);
        currentUserId = preferences.getInt("userId", -1);

        //TODO: check If no user is logged in
        if (currentUserId == -1) {
            redirectToLogin();
            return;
        }

        initializeViews();
        setUpListeners();
        setupPasswordValidation();
        loadUserData();

        return root;
    }

    private void initializeViews() {
        tilName = binding.tilName;
        tilSurname = binding.tilSurname;
        tilCurrentPassword = binding.tilCurrentPassword;
        tilNewPassword = binding.tilNewPassword;
        tilConfirmNewPassword = binding.tilConfirmNewPassword;

        etUsername = binding.etUsername;
        etName = binding.etName;
        etSurname = binding.etSurname;
        etEmail = binding.etEmail;
        etAge =  binding.etAge;
        etCurrentPassword = binding.etCurrentPassword;
        etNewPassword = binding.etNewPassword;
        etConfirmNewPassword = binding.etConfirmNewPassword;

        btnSaveProfile = binding.btnSaveProfile;
        btnChangePassword = binding.btnChangePassword;
        btnLogout = binding.btnLogout;
        btnDeleteAccount = binding.btnDeleteAccount;
        progressBar = binding.progressBar;

        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void setUpListeners() {
        btnSaveProfile.setOnClickListener(v -> saveProfile());
        btnChangePassword.setOnClickListener(v -> changePassword());
        btnLogout.setOnClickListener(v -> showLogoutDialog());
        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
    }

    private void saveProfile() {
        if (!isProcessing.compareAndSet(false, true)) {
            showToast("Please wait...");
            return;
        }

        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String surname = etSurname.getText() != null ? etSurname.getText().toString().trim() : "";

        //Clear errors
        tilName.setError(null);
        tilSurname.setError(null);

        //Validate
        boolean hasError = false;

        if (name.length() > 20 || name.length() < 2) {
            tilName.setError("Name too long (max 20 characters)");
            hasError = true;
        }

        if (name.length() < 3) {
            tilName.setError("Name too short (min 3 characters)");
            hasError = true;
        }

        if (surname.length() > 30) {
            tilName.setError("Surname too long (max 30 characters)");
            hasError = true;
        }

        if (surname.length() < 3) {
            tilName.setError("Surname too short (min 3 characters)");
            hasError = true;
        }

        if (hasError) {
            isProcessing.set(false);
            return;
        }

        setLoadingState(true);

        executorService.execute(() -> {
            try {
                database.userDataDao().updateProfile(currentUserId, name, surname);
                if (!getActivity().isFinishing() && !getActivity().isDestroyed()) {
                    mainHandler.post(() -> {
                        showToast("Profile updated successfully!");
                        loadUserData(); //Reload to confirm
                        isProcessing.set(false);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    showToast("Error updating profile");
                    setLoadingState(false);
                    isProcessing.set(false);
                });
            }
        });
    }

    //TODO: check old password equal to the new one
    private void changePassword() {
        if (!isProcessing.compareAndSet(false, true)) {
            showToast("Please wait...");
            return;
        }

        String currentPassword = etCurrentPassword.getText() != null ? etCurrentPassword.getText().toString().trim() : "";
        String newPassword = etNewPassword.getText() != null ? etNewPassword.getText().toString().trim() : "";
        String confirmNewPassword = etConfirmNewPassword.getText() != null ? etConfirmNewPassword.getText().toString().trim() : "";

        //Clear errors
        tilCurrentPassword.setError(null);
        tilNewPassword.setError(null);
        tilConfirmNewPassword.setError(null);

        //Validate
        boolean hasErrors = false;

        if (currentPassword.isEmpty()) {
            tilCurrentPassword.setError("Enter current password");
            hasErrors = true;
        }

        if (newPassword.isEmpty()) {
            tilNewPassword.setError("Enter current password");
            hasErrors = true;
        } else if (newPassword.length() < 8) {
            tilNewPassword.setError("Passowrd must be at least 8 characters");
            hasErrors = true;
        }

        if (confirmNewPassword.isEmpty()) {
            tilConfirmNewPassword.setError("Confirm new password");
            hasErrors = true;
        } else if (!newPassword.equals(confirmNewPassword)){
            tilConfirmNewPassword.setError("Passwords do not match");
            hasErrors = true;
        }

        if (hasErrors) {
            isProcessing.set(false);
            return;
        }

        setLoadingState(true);

        executorService.execute(() -> {
            try {
                if (!currentUser.getPassword().equals(currentPassword)) {
                    mainHandler.post(() -> {
                        tilCurrentPassword.setError("Incorrect password");
                        setLoadingState(false);
                        isProcessing.set(false);
                    });
                    return;
                }
                database.userDataDao().updatePassword(currentUserId, newPassword);

                if (!getActivity().isFinishing() && getActivity().isDestroyed()) {
                    mainHandler.post(() -> {
                        showToast("Password changed successfully");
                        setLoadingState(false);
                        isProcessing.set(false);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    showToast("Error changing password");
                    setLoadingState(false);
                    isProcessing.set(false);
                });
            }
        });
    }

    //Check null pointer exception
    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logout() {
        if (!isProcessing.compareAndSet(false, true)) {
            return;
        }

        executorService.execute(() -> {
            try {
                //Clear session
                SharedPreferences preferences = getActivity().getSharedPreferences("UserPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.clear();
                editor.apply();

                Thread.sleep(200);

                if (!getActivity().isFinishing() && !getActivity().isDestroyed()) {
                    mainHandler.post(() -> {
                        showToast("Logged put successfully");
                        redirectToLogin();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    showToast("Logout error");
                    isProcessing.set(false);
                });
            }
        });
    }

    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone!")
                .setPositiveButton("Delete", (dialog, which) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAccount() {
        if (!isProcessing.compareAndSet(false, true)) {
            return;
        }

        setLoadingState(true);

        executorService.execute(() -> {
            try {
                // Delete user from database
                database.userDataDao().delete(currentUser);

                // Clear session
                SharedPreferences prefs = getActivity().getSharedPreferences("UserPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.clear();
                editor.apply();

                if (!getActivity().isFinishing() && !getActivity().isDestroyed()) {
                    mainHandler.post(() -> {
                        showToast("Account deleted successfully");
                        redirectToLogin();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    showToast("Error deleting account");
                    setLoadingState(false);
                    isProcessing.set(false);
                });
            }
        });
    }

    private void redirectToLogin() {
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void clearPasswordFields() {
        if (etCurrentPassword != null) etCurrentPassword.setText("");
        if (etNewPassword != null) etNewPassword.setText("");
        if (etConfirmNewPassword != null) etConfirmNewPassword.setText("");
    }

    private void setLoadingState(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }

        if (btnSaveProfile != null) btnSaveProfile.setEnabled(!isLoading);
        if (btnChangePassword != null) btnChangePassword.setEnabled(!isLoading);
        if (btnLogout != null) btnLogout.setEnabled(!isLoading);
        if (btnDeleteAccount != null) btnDeleteAccount.setEnabled(!isLoading);

        if (etName != null) etName.setEnabled(!isLoading);
        if (etSurname != null) etSurname.setEnabled(!isLoading);
        if (etCurrentPassword != null) etCurrentPassword.setEnabled(!isLoading);
        if (etNewPassword != null) etNewPassword.setEnabled(!isLoading);
        if (etConfirmNewPassword != null) etConfirmNewPassword.setEnabled(!isLoading);
    }

    private void showToast(String message) {
        if (!getActivity().isFinishing() && !getActivity().isDestroyed()) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;

        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }

        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }

        isProcessing.set(false);
    }
}