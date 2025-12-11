package com.example.navigationbarstarter.ui.settings;

import static android.content.Context.MODE_PRIVATE;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.navigationbarstarter.R;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

public class ChangePasswordDialog extends DialogFragment {

    private TextInputEditText etOldPassword, etNewPassword, etConfirmNewPassword;
    private Button btnSave, btnClose;
    private long userId;

    private SettingsViewModel settingsViewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_change_password, container, false);

        // Get ViewModel scoped to parent fragment
        settingsViewModel = new ViewModelProvider(requireParentFragment())
                .get(SettingsViewModel.class);

        setUpComponent(view);
        getUserIdFromSharedPreferences();
        observePasswordResult();
        setUpButtonListener();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        Dialog dialog = getDialog();
        if (dialog != null) {
            // Centered dialog with wrap_content
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
        }
    }

    // -----------------------------
    // UI & Logic
    // -----------------------------
    private void setUpComponent(View view) {
        etOldPassword = view.findViewById(R.id.etOldPassword);
        etNewPassword = view.findViewById(R.id.etNewPassword);
        etConfirmNewPassword = view.findViewById(R.id.etConfirmNewPassword);
        btnSave = view.findViewById(R.id.btnSave);
        btnClose = view.findViewById(R.id.btnClose);
    }

    private void getUserIdFromSharedPreferences() {
        SharedPreferences sharedPreferences = requireActivity()
                .getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userId = sharedPreferences.getLong("userId", -1);
    }

//    private void loadOldPassword(UserData userData) {
//        etOldPassword.setText(userData.getPassword());
//    }

    private void observePasswordResult() {
        settingsViewModel.getEqualPasswordLiveData().observe(this, result -> {

            etOldPassword.setError(null);
            etNewPassword.setError(null);
            etConfirmNewPassword.setError(null);

            switch (result) {
                case 1: // new == old
                    etNewPassword.setError("New password cannot be equal to old password");
                    break;

                case 2: // mismatch confirm
                    etNewPassword.setError("Passwords do not match");
                    etConfirmNewPassword.setError("Passwords do not match");
                    break;

                case 3: // regex rules
                    etNewPassword.setError("Password must have 1 uppercase, 1 special character, 8+ chars");
                    break;

                case 4: // incorrect old password
                    etOldPassword.setError("Old password is incorrect");
                    break;

                case 0: // success
                    Toast.makeText(getContext(), "Password updated!", Toast.LENGTH_SHORT).show();
                    dismiss();
                    break;
            }
        });
    }

    private void setUpButtonListener() {
        btnClose.setOnClickListener(v -> dismiss());

        btnSave.setOnClickListener(v -> {
            String oldPass = Objects.requireNonNull(etOldPassword.getText()).toString().trim();
            String newPass = Objects.requireNonNull(etNewPassword.getText()).toString().trim();
            String confirmPass = Objects.requireNonNull(etConfirmNewPassword.getText()).toString().trim();

            settingsViewModel.checkAndUpdatePassword(userId, oldPass, newPass, confirmPass);
        });
    }
}
