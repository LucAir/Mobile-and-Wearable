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
    public void checkUsernameUnique(String username) {
        repository.isUsernameUnique(username, isUnique ->
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
    public void updateUserFields(long id, String name, String surname) {
        repository.updateUserFields(id, name, surname);
    }




}