package com.example.navigationbarstarter.database.session;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

public class SessionViewModel extends AndroidViewModel {

    private final MutableLiveData<Boolean> saveSessionResult = new MutableLiveData<>();
    private final MutableLiveData<List<List<String>>> comparedSessions =
            new MutableLiveData<>();

    private final SessionRepository  repository;

    public SessionViewModel(@NonNull Application application) {
        super(application);
        repository = new SessionRepository(application);
    }

    public void saveSession(long userId, List<String> sessionTS) {
        repository.saveSession(userId, sessionTS, success -> {
            saveSessionResult.postValue((Boolean) success);
            if (Boolean.TRUE.equals(success)) {
                //Reload session to update live data
                loadComparedSessions(userId);
            }
        });
    }

    public LiveData<Boolean> getSaveSessionResults() {
        return saveSessionResult;
    }

    public LiveData<List<List<String>>> getComparedSessions() {
        return comparedSessions;
    }

    public void loadComparedSessions(long userId) {
        // Post to LiveData on main thread
        repository.getSessionsGroupedByIndex(userId, false, comparedSessions::postValue);
    }

    public void loadComparedSessions(long userId, boolean ascending) {
        repository.getSessionsGroupedByIndex(userId, ascending, comparedSessions::postValue);
    }

}
