package com.example.navigationbarstarter.database.session;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

public class SessionViewModel extends AndroidViewModel {

    private final MutableLiveData<Boolean> saveSessionResult = new MutableLiveData<>();

    private final SessionRepository  repository;

    public SessionViewModel(@NonNull Application application) {
        super(application);
        repository = new SessionRepository(application);
    }

    public void saveSession(long userId, List<String> sessionTS) {
        repository.saveSession(userId, sessionTS, success ->
                saveSessionResult.postValue((Boolean) success));
    }

    public LiveData<Boolean> getSaveSessionResults() {
        return saveSessionResult;
    }
}
