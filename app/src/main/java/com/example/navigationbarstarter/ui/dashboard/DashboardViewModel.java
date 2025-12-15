package com.example.navigationbarstarter.ui.dashboard;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class DashboardViewModel extends AndroidViewModel {

    private final MutableLiveData<Float> userBaselineHr = new MutableLiveData<>();
    private final MutableLiveData<Float> userBaselineHrv = new MutableLiveData<>();
    private final DashboardRepository repository;

    public DashboardViewModel(Application application) {
        super(application);
        repository = new DashboardRepository(application);
    }

    /**
     * Returns baseline hr for a give user, and post value inside the live variable
     * @param userId
     */
    public void getBaselineHr(Long userId) {
        repository.getUserBaselineHr(userId, baselineHr ->
                userBaselineHr.postValue((Float) baselineHr));
    }

    public LiveData<Float> getUserBaselineHr() {
        return userBaselineHr;
    }

    /**
     * Returns baseline hrv for a give user, and post value inside the live variable
     * @param userId
     */
    public void getBaselineHrv(Long userId) {
        repository.getUserBaselineHrv(userId, baselineHrv ->
                userBaselineHrv.postValue((Float) baselineHrv));
    }

    public LiveData<Float> getUserBaselineHrv() {
        return userBaselineHrv;
    }

}