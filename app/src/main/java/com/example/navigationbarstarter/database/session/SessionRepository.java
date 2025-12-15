package com.example.navigationbarstarter.database.session;

import android.app.Application;
import android.util.Log;

import com.example.navigationbarstarter.database.AppDatabase;
import com.example.navigationbarstarter.database.UserDataDao;

import java.util.ArrayList;
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

    /**
     * This method is used to save session. Take 3 parameters:
     * @param userId: user ID
     * @param sessionTS: list of timestamp
     * @param callback
     * Session are taken from a file that you can find under res/raw/ten_session.csv
     * Each session is composed of 3600 points that will be sampled in 60
     * @see com.example.navigationbarstarter.data.HeartRateVariability
     * So we take one session from the file and save the timestamp of when the user stop the focus
     * timer. So everything is a simulation in here, but we save timestamp as a real and good practice.
     */
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

    /**
     * This method is used to retrieve the last 2 session ordered in ASC or DESC order based on timestamp
     * @param userId: user ID
     * @param ascending: boolean to understand order
     * @param callback
     * We have 2 chart in the code, but we still retrieve data from this method. In the linechart, we display
     * only the last session -> we access to the list using 1 as index.
     */
    public void getSessionsGroupedByIndex(
            long userId,
            boolean ascending,
            ResultCallback<List<List<String>>> callback
    ) {
        executor.execute(() -> {
            List<SessionData> sessions = ascending
                    ? sessionDataDao.getSessionsByUserAsc(userId)
                    : sessionDataDao.getSessionsByUserDesc(userId);

            //Group each session's timestamps into List<List<String>>
            List<List<String>> groupedSessions = new ArrayList<>();
            for (SessionData session : sessions) {
                if (session.getSessionTS() != null && !session.getSessionTS().isEmpty()) {
                    groupedSessions.add(session.getSessionTS());
                }
            }

            callback.onResult(groupedSessions);
        });
    }

    public interface ResultCallback<T> {
        void onResult(T isUnique);
    }

}
