package com.example.navigationbarstarter.ui.dashboard;

import android.content.SharedPreferences;
import android.util.Log;

public class FakeMLAlgorithm {
    /**
     * In this class we wanted to implement an ML algorithm for stress detection given
     * heartbeat and HRV.
     *
     * After the last meeting with Professor and TA, following their tips, we decided to implement
     * a "Fake ML Algorithm" which basically is are some IF-ELSE nested.
     *
     * Just to explain:
     * HRV is where the amount of time between your heartbeats fluctuates slightly.
     * HIGH HRV means that you body can adapt to many kinds of changes -> usually this kind
     * of people are less stressed and happier.
     * LOW HRV shows that your body is less resilient and struggles to handle changing situations.
     * It's common when heartbeats are high, because there is less time between heartbeats
     *
     * HIGH heartbeats represents a situation where you are active  / stressed / in danger
     * LOW heartbeats represents a situation where you are resting / relaxed
     *
     * Before reading the code here are the 4 division:
     * 0)HIGH heart-rate variability and LOW heart-rate -> OPTIMAL STATE → PERFECT!
     * 1)HIGH heart-rate variability and HIGH heart-rate -> ACTIVE AND ENGAGED, but MONITOR (high heartbeats)
     * 2)LOW heart-rate variability and LOW heart-rate -> BREAK RECOMMENDED -> low energy and poor adaptability -> mental fatigue
     * 3)LOW heart-rate variability and HIGH heart-rate and -> STRESSED (WORSE SITUATION) -> BREAK
     *
     * Level 1 and 2 in the chart are handle with the same yellow color. Are considered as warnings.
     */

    public static final int STRESS_CRITICAL = 3;
    public static final int STRESS_BREAK_RECOMMENDED = 2;
    public static final int STRESS_MONITOR = 1;
    public static final int STRESS_OPTIMAL_STATE = 0;

    public static int detectStressLevel(int heartRate, double hrv, int baseline_hr, float baseline_hrv) {
        //Determine if heart-rate is high relative to user's baseline
        boolean isHeartRateHigh = heartRate > (baseline_hr + 15);
        boolean isHRVLow = hrv < (baseline_hrv * 0.7);

        Log.d("StressDebug", "BPM=" + heartRate + " HRV=" + hrv + " HRbaseline=" + baseline_hr + " HRVbaseline=" + baseline_hrv);

        //CRITICAL STRESS
        if (isHeartRateHigh && isHRVLow) {
            return STRESS_CRITICAL;
        } else if (!isHeartRateHigh && isHRVLow) {
            return STRESS_BREAK_RECOMMENDED;
        } else if(isHeartRateHigh && !isHRVLow) {
            return STRESS_MONITOR;
        } else {
            return STRESS_OPTIMAL_STATE;
        }
    }
}
