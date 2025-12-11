package com.example.navigationbarstarter.ui.settings;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.navigationbarstarter.database.AppDatabase;
import com.example.navigationbarstarter.database.UserData;

public class SettingsViewModel extends AndroidViewModel {

    private final SettingsRepository repository;

    private final MutableLiveData<UserData> currentUserInfo = new MutableLiveData<>();
    private final MutableLiveData<Boolean> usernameUniqueLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> emailUniqueLiveData = new MutableLiveData<>();

    //Here we post false when old password is equal to the new one and when the 2 new password are ≠
    private final MutableLiveData<Integer> equalPasswordLiveData = new MutableLiveData<>();

    //Constructor
    public SettingsViewModel(Application application) {
        super(application);
        repository = new SettingsRepository(application);
    }

    /*
     * Check if username is unique
     * and
     * post value inside the live variable
     */
    public void isUsernameUniqueAndUpdate(long userId, String username) {
        repository.isUsernameUniqueAndUpdate(userId, username, isUnique ->
                usernameUniqueLiveData.postValue((boolean) isUnique));
    }

    public LiveData<Boolean> getUsernameUniqueLiveData() {
        return usernameUniqueLiveData;
    }

    /*
     * Retrieving user data associated to user id
     * and
     * post value inside the live variable
     */
    public void getUserData(Long userId) {
        repository.getUserData(userId, userData ->
                currentUserInfo.postValue((UserData) userData));
    }

    public LiveData<UserData> getUserInfo() {
        return currentUserInfo;
    }

    /*
     * Used to update nullable fields
     */
    public void updateUserFields(long id, String name, String surname, String uri) {
        repository.updateUserFields(id, name, surname, uri);
    }

    /*
     * Check if email is unique
     * and
     * post the value inside the live variable
     */
    public void isEmailUniqueAndUpdate(long userId, String email) {
        repository.isEmailUniqueAndUpdate(userId, email, isUnique -> {
            emailUniqueLiveData.postValue((boolean) isUnique);
        });
    }

    public LiveData<Boolean> getEmailUniqueLiveData() {
        return emailUniqueLiveData;
    }


    /*
     * Check if new password is equal to the one store in DB
     * OR
     * Check if new password and currentNewPassword are different
     * AND
     * If everything is fine update the password
     */
    public void checkAndUpdatePassword(long userId, String oldPassword, String newPassword, String confirmNewPassword) {
        repository.isPasswordCorrectAndUpdate(userId, oldPassword, newPassword, confirmNewPassword, result ->
                equalPasswordLiveData.postValue((Integer) result));
    }

    public LiveData<Integer> getEqualPasswordLiveData() {
        return equalPasswordLiveData;
    }
}