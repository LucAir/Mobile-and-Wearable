package com.example.navigationbarstarter.ui.dashboard;

import android.app.Application;

import com.example.navigationbarstarter.database.AppDatabase;
import com.example.navigationbarstarter.database.UserData;
import com.example.navigationbarstarter.database.UserDataDao;
import com.example.navigationbarstarter.ui.settings.SettingsRepository;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DashboardRepository {

    private final UserDataDao userDataDao;

    private final Executor executor;

    public DashboardRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        userDataDao = db.userDataDao();
        executor = Executors.newSingleThreadExecutor();
    }

    public void getUserBaselineHr(Long id, DashboardRepository.ResultCallback callback) {
        executor.execute(() -> {
            UserData userData = userDataDao.getUserById(id);
            userData.setBaselineHr(65f);
            userData.setBaselineHrv(70f);
            userDataDao.updateUser(userData);
            float baselineHr = userData.getBaselineHr();
            callback.onResult(baselineHr);
        });
    }

    public void getUserBaselineHrv(Long id, DashboardRepository.ResultCallback callback) {
        executor.execute(() -> {
            UserData userData = userDataDao.getUserById(id);
            float baselineHrv = userData.getBaselineHrv();
            callback.onResult(baselineHrv);
        });
    }
    public interface ResultCallback<T> {
        void onResult(T isUnique);
    }
}

