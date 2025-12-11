package com.example.navigationbarstarter.data;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HeartRateVariability {

    private static final SimpleDateFormat sdf =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS", Locale.US);

    public static float rrToBpm(float rrMs) {
        return 60000f / rrMs;
    }

    public static List<Float> computeBPM(List<String> timestamps) {
        List<Float> heartbeatBpmValues = new ArrayList<>();
        if (timestamps.size() < 2) return heartbeatBpmValues;

        // compute per-beat BPM
        for (int i = 1; i < timestamps.size(); i++) {
            try {
                long t1 = sdf.parse(timestamps.get(i - 1)).getTime();
                long t2 = sdf.parse(timestamps.get(i)).getTime();
                float rr = (float) (t2 - t1);
                if (rr > 250 && rr < 2000) heartbeatBpmValues.add(rrToBpm(rr));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        // aggregate into 60 points (1 per minute)
        List<Float> perMinuteBpm = new ArrayList<>();
        int totalBeats = heartbeatBpmValues.size();
        if (totalBeats == 0) return perMinuteBpm;
        int pointsPerMinute = totalBeats / 60;

        for (int i = 0; i < 60; i++) {
            int start = i * pointsPerMinute;
            int end = (i == 59) ? totalBeats : start + pointsPerMinute;
            if (start >= totalBeats) break;

            float sum = 0f;
            for (int j = start; j < end; j++) sum += heartbeatBpmValues.get(j);
            perMinuteBpm.add(sum / (end - start));
        }

        return perMinuteBpm;
    }

    public static List<Float> computeHRV(List<String> timestamps) {
        List<Float> rrList = new ArrayList<>();
        if (timestamps.size() < 3) return new ArrayList<>();

        // compute RR intervals
        for (int i = 1; i < timestamps.size(); i++) {
            try {
                long t1 = sdf.parse(timestamps.get(i - 1)).getTime();
                long t2 = sdf.parse(timestamps.get(i)).getTime();
                float rr = (float) (t2 - t1);
                if (rr > 250 && rr < 2000) rrList.add(rr);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        // compute RMSSD using sliding window (~30 beats)
        List<Float> rmssdValues = new ArrayList<>();
        int window = 30;
        for (int i = window; i < rrList.size(); i++) {
            List<Float> segment = rrList.subList(i - window, i);
            rmssdValues.add(calculateRMSSD(segment));
        }

        // scale RMSSD to 60 points
        List<Float> perMinuteHRV = new ArrayList<>();
        int totalPoints = rmssdValues.size();
        if (totalPoints == 0) return perMinuteHRV;
        int pointsPerMinute = totalPoints / 60;

        for (int i = 0; i < 60; i++) {
            int start = i * pointsPerMinute;
            int end = (i == 59) ? totalPoints : start + pointsPerMinute;
            if (start >= totalPoints) break;

            float sum = 0f;
            for (int j = start; j < end; j++) sum += rmssdValues.get(j);
            perMinuteHRV.add(sum / (end - start));
        }

        return perMinuteHRV;
    }

    private static float calculateRMSSD(List<Float> rrList) {
        if (rrList.size() < 2) return 0;
        float sumSquares = 0;
        for (int i = 1; i < rrList.size(); i++) {
            float diff = rrList.get(i) - rrList.get(i - 1);
            sumSquares += diff * diff;
        }
        return (float) Math.sqrt(sumSquares / (rrList.size() - 1));
    }
}
