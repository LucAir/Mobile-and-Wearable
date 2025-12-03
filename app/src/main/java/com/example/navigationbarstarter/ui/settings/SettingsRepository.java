package com.example.navigationbarstarter.ui.settings;

import android.app.Application;

import com.example.navigationbarstarter.database.AppDatabase;
import com.example.navigationbarstarter.database.UserData;
import com.example.navigationbarstarter.database.UserDataDao;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SettingsRepository {
    private final UserDataDao userDataDao;

    private final Executor executor;

    public SettingsRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        userDataDao = db.userDataDao();
        executor = Executors.newSingleThreadExecutor();
    }

    //Get user data given the user ID
    public void getUserData(Long id, ResultCallback callback) {
        executor.execute(() -> {
            UserData userData = userDataDao.getUserById(id);
            callback.onResult(userData);
        });
    }


    //Check if the new username exists in the DB
    public void isUsernameUnique(String username, ResultCallback callback) {
        executor.execute(() -> {
            UserData user = userDataDao.getUserByUsername(username);
            boolean isUnique = (user == null);
            callback.onResult(isUnique);
        });
    }

    //Update user field like name and surname
    public void updateUserFields(long userId, String name, String surname) {
        executor.execute(() -> {
            UserData userData = userDataDao.getUserById(userId);
            userData.setName(name);
            userData.setSurname(surname);
            userDataDao.updateUser(userData);
        });
    }

    public void insertOrUpdateImage( ResultCallback callback) {

    }















    public interface ResultCallback<T> {
        void onResult(T isUnique);
    }
}
