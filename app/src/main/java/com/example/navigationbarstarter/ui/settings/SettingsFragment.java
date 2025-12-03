package com.example.navigationbarstarter.ui.settings;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ObjectListKt;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.navigationbarstarter.R;
import com.example.navigationbarstarter.database.UserData;
import com.example.navigationbarstarter.databinding.FragmentSettingsBinding;
import com.example.navigationbarstarter.ui.access.LoginActivity;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private TextView settingsTitle;
    private ImageView ivProfileImage;
    private TextInputEditText etName, etSurname, etEmail, etUsername;
    private Button btnChangePassword, btnSave, btnLogout;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private Uri selectedImageUri; // store the selected URI
    private long userId;

    /*
     * Fragment -> handles user interaction (clicks, reading text)
     * ViewModel -> Runs business logic (validation, logic, decisions)
     * Repository -> Handles ONLY database operations
     */

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        SettingsViewModel settingsViewModel =
                new ViewModelProvider(this).get(SettingsViewModel.class);
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        //Initialize image picker launcher
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if(result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        ivProfileImage.setImageURI(selectedImageUri);
                    }
                }
        );

        //Checking if the live variable has changed. If we have the value (UserData) use it to load information on UI
        settingsViewModel.getUserInfo().observe(getViewLifecycleOwner(), userData -> {
            if(userData == null) {
                Toast.makeText(getContext(), "Impossible to retrieve information about the user", Toast.LENGTH_SHORT).show();
            } else {
                loadCurrentData(userData);
            }
        });

        //Checking if the username the user has changed is unique. If not show an error.
        settingsViewModel.getUsernameUniqueLiveData().observe(getViewLifecycleOwner(), isUnique -> {
            if(!isUnique) {
                etUsername.setError("Username already exists");
            }
        });

        //Setting bindings with xml
        setBinding();

        //Take user ID
        getUserIdFromSharedPreferences();

        //Take userData
        settingsViewModel.getUserData(userId);

        //SetUp button listener
        setUpButton(settingsViewModel);

        return root;
    }

    //Binding view element to variables
    private void setBinding() {
        settingsTitle = binding.settingsTitle;
        ivProfileImage = binding.ivProfileImage;
        etName = binding.etName;
        etSurname = binding.etSurname;
        etUsername = binding.etUsername;
        etEmail = binding.etEmail;
        btnChangePassword = binding.btnChangePassword;
        btnSave = binding.btnSave;
        btnLogout = binding.btnLogout;
    }

    //Load user data inside the corresponding fields
    private void loadCurrentData(UserData userData) {
        etName.setText(userData.getName());
        etSurname.setText(userData.getSurname());
        etUsername.setText(userData.getUsername());
        etEmail.setText(userData.getEmail());

        //Load profile picture
        String imageUri = userData.getProfileImageUri();
        if(imageUri != null && !imageUri.isEmpty()) {
            ivProfileImage.setImageURI(Uri.parse(imageUri));
        } else {
            ivProfileImage.setImageResource(R.drawable.ic_default_avatar);
        }
    }

    //Load userId from shared preferences
    private void getUserIdFromSharedPreferences() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userId = sharedPreferences.getLong("userId", -1);
    }

    //Logout logic
    private void logout() {
        SharedPreferences preferences = requireActivity().getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();

        startActivity(new Intent(getActivity(), LoginActivity.class));
        requireActivity().finish();
    }

    //Set up Button listener
    private void setUpButton(SettingsViewModel settingsViewModel) {
        btnLogout.setOnClickListener(v -> logout());

        btnSave.setOnClickListener(v -> {
            //Getting field values
            String newUri = selectedImageUri != null ? selectedImageUri.toString() : null;
            String newName = Objects.requireNonNull(etName.getText()).toString().trim();
            String newSurname = Objects.requireNonNull(etSurname.getText()).toString().trim();
            String newUsername = Objects.requireNonNull(etUsername.getText()).toString().trim();

            //Send data to ViewModel
            settingsViewModel.updateUserFields(userId, newName, newSurname, newUri);

            //Checking validity of Username -> MUST NOT BE an EQUAL username in the DB
            settingsViewModel.isUsernameUniqueAndUpdate(userId, newUsername);

            Toast.makeText(getContext(), "New settings saved successfully", Toast.LENGTH_SHORT).show();
        });

        //This is not a button, but works the same
        ivProfileImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}