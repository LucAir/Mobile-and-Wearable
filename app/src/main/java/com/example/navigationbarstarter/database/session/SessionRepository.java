package com.example.navigationbarstarter.database.session;

import android.app.Application;

import com.example.navigationbarstarter.database.AppDatabase;
import com.example.navigationbarstarter.database.UserData;
import com.example.navigationbarstarter.database.UserDataDao;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SessionRepository {
    private final SessionDataDao sessionDataDao;
    private final Executor executor;
    private final UserDataDao userDataDao;

    public SessionRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        sessionDataDao = db.sessionDataDao();
        userDataDao = db.userDataDao();
        executor = Executors.newSingleThreadExecutor();
    }

    public void saveSession(Long userId, List<String> sessionTS, ResultCallback callback) {
        executor.execute(() -> {
            try {
                SessionData sessionData = new SessionData();
                sessionData.setUserId(userId);
                sessionData.setSessionTS(sessionTS);
                sessionData.setCreatedAt(System.currentTimeMillis());
                long sessionId = sessionDataDao.insertSession(sessionData);

                boolean success = sessionId > 0;
                callback.onResult(success);
            } catch (Exception e) {
                e.printStackTrace();
                callback.onResult(false);
            }
        });
    }


    public interface ResultCallback<T> {
        void onResult(T isUnique);
    }

}
