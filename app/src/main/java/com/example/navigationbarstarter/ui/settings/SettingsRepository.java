package com.example.navigationbarstarter.ui.settings;

import android.app.Application;

import com.example.navigationbarstarter.database.AppDatabase;
import com.example.navigationbarstarter.database.UserData;
import com.example.navigationbarstarter.database.UserDataDao;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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


    //Check if the new username exists in the DB and update
    public void isUsernameUniqueAndUpdate(long userId, String username, ResultCallback callback) {
        executor.execute(() -> {
            UserData userEdit = userDataDao.getUserById(userId);
            UserData user = userDataDao.getUserByUsernameExcludingCurrent(username, userId);
            boolean isUnique = (user == null);
            if (isUnique) {
                userEdit.setUsername(username);
                userDataDao.updateUser(userEdit);
            }
            callback.onResult(isUnique);
        });
    }

    //Check if the new email exists in the DB and update
    public void isEmailUniqueAndUpdate(long userId, String email, ResultCallback callback) {
        executor.execute(() -> {
            UserData userEdit = userDataDao.getUserById(userId);
            UserData user = userDataDao.getUserByEmailExcludingCurrent(email, userId);
            boolean isUnique = (user == null);
            if (isUnique) {
                userEdit.setEmail(email);
                userDataDao.updateUser(userEdit);
            }
            callback.onResult(isUnique);
        });
    }

    //Update user field like name, surname, image
    public void updateUserFields(long userId, String name, String surname, String uri) {
        executor.execute(() -> {
            UserData userData = userDataDao.getUserById(userId);
            userData.setName(name);
            userData.setSurname(surname);
            userData.setProfileImageUri(uri);
            userDataDao.updateUser(userData);
        });
    }

    //Here we return a integer to customize error panel in UI
    public void isPasswordCorrectAndUpdate(long userId,
                                           String oldPassword,
                                           String newPassword,
                                           String confirmNewPassword,
                                           ResultCallback callback) {

        executor.execute(() -> {
            int error = 0;
            UserData userEdit = userDataDao.getUserById(userId);
            String oldPasswordInDb = userEdit.getPassword();

            //Regex check
            Pattern pattern = Pattern.compile("^(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{8,}$");
            Matcher matcher = pattern.matcher(newPassword);

            if (!matcher.matches()) {
                error = 3;   //regex failed
            }
            else if (!oldPassword.equals(oldPasswordInDb)) {
                error = 4;   //old password is wrong
            }
            else if (newPassword.equals(oldPasswordInDb)) {
                error = 1;   //new pass = old pass
            }
            else if (!newPassword.equals(confirmNewPassword)) {
                error = 2;   //new != confirm
            }
            else {
                error = 0;
                userEdit.setPassword(newPassword);
                userDataDao.updateUser(userEdit);
            }

            callback.onResult(error);
        });
    }


    public interface ResultCallback<T> {
        void onResult(T isUnique);
    }
}
