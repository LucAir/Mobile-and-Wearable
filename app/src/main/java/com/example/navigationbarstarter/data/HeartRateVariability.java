package com.example.navigationbarstarter.data;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HeartRateVariability {

    /**
     * This class takes a list of heartbeat timestamps (each timestamp is the exact moment when a heartbeat occurred)
     * and converts them into two useful physiological metrics:
     *  1) BPM (Beats Per Minute)
     *  2) HRV (Heart Rate Variability), specifically RMSSD
     * Since raw heartbeat timestamps are NOT directly usable for charts, this class transforms them
     * into exactly 60 averaged values—one for each minute of a 1-hour recording window.
     * This makes the data easy to visualize in a mobile chart.
     *
     * Displaying thousands of points on a chart is bad for Android.
     * The solution:
     *  - All BPM values are divided into 60 equal-sized chunks.
     *  - Each chunk represents one minute of data.
     *  - Inside each minute, all BPM values are averaged.
     *
     * Computing HVR using RMSSD:
     *  - Extracting RR intervals used the same intervals as for BPM
     *  - Applying a sliding window of 30 beats -> for each group of 30 RR intervals we compute RMSSD
     * We get a list of HRV similar in length to the number of beats
     *
     * Like BPM, the HRV values are too many.
     * To fit a chart:
     *  - HRV values are also split into 60 equal batches
     *  - Each batch is averaged
     *  - Produces 60 HRV data points
     * Now BPM and HRV line up perfectly in time.
     */

    /**
     * Java SimpleDateFormat cannot parse microseconds (6 digits),
     * only milliseconds (3 digits).
     *
     * We use SDF for the base timestamp, then manually add microseconds.
     */
    private static final SimpleDateFormat sdf =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS", Locale.US);

    /**
     * Convert RR interval (ms) to BPM.
     * This gives a BPM value for each heartbeat, not per minute yet.
     * So if we have 3600 heartbeats, we have 3600 BPM values.
     */
    public static float rrToBpm(float rrMs) {
        return 60000f / rrMs;
    }

    /**
     * Parse timestamp with microseconds correctly.
     * Example input: "2025-12-11 12:53:04.849629"
     */
    private static long parseTimestampMicros(String ts) throws ParseException {

        //Split second and microsecond part
        String[] parts = ts.split("//.");

        long baseMs = sdf.parse(parts[0]).getTime(); //ms

        long micros = 0;
        if(parts.length == 2) {
            String microStr = parts[1];
            if (microStr.length() > 6) {
                microStr = microStr.substring(0, 6); //trim if needed
            }
            micros = Long.parseLong(microStr);
        }
        return baseMs * 1000 + micros;
    }


    /**
     * Compute BPM per beat, then aggregate into 60 points (1 per minute).
     */
    public static List<Float> computeBPM(List<String> timestamps) {
        List<Float> heartbeatBpmValues = new ArrayList<>();
        if (timestamps.size() < 2) return heartbeatBpmValues;

        //Compute per-beat BPM
        for (int i = 1; i < timestamps.size(); i++) {
            try {
                long t1 = parseTimestampMicros(timestamps.get(i - 1));
                long t2 = parseTimestampMicros(timestamps.get(i));

                float rrMs = (t2 - t1) / 1000f;

                //Keep only valid RR intervals
                if (rrMs > 250 && rrMs < 2000) {
                    heartbeatBpmValues.add(rrToBpm(rrMs));
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        //Aggregate into 60 points (1 per minute)
        List<Float> perMinuteBpm = new ArrayList<>();
        int count = heartbeatBpmValues.size();
        if (count == 0) return perMinuteBpm;

        int chunk = Math.max(1, count / 60);

        for (int i = 0; i < 60 && i  * chunk < count; i++) {
            int start = i * chunk;
           int end = Math.min(count, start + chunk);

            float sum = 0f;

            for (int j = start; j < end; j++) sum += heartbeatBpmValues.get(j);
            perMinuteBpm.add(sum / (end - start));
        }

        return perMinuteBpm;
    }

    /**
     * Compute HRV (RMSSD) using 30-beat sliding window,
     * then compress result to 60 boxes.
     */
    public static List<Float> computeHRV(List<String> timestamps) {
        List<Float> rrList = new ArrayList<>();
        if (timestamps.size() < 3) return new ArrayList<>();

        //Compute RR intervals
        for (int i = 1; i < timestamps.size(); i++) {
            try {
                long t1 = parseTimestampMicros(timestamps.get(i - 1));
                long t2 = parseTimestampMicros(timestamps.get(i));

                float rrMs = (t2 - t1) / 1000f;

                if (rrMs > 250 && rrMs < 2000){
                    rrList.add(rrMs);
                }

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        //Compute sliding RMSSD
        List<Float> rmssdValues = new ArrayList<>();
        int window = 30;

        for (int i = window; i < rrList.size(); i++) {
            List<Float> segment = rrList.subList(i - window, i);
            rmssdValues.add(calculateRMSSD(segment));
        }

        //Scale RMSSD to 60 points
        List<Float> perMinuteHRV = new ArrayList<>();
        int totalPoints = rmssdValues.size();
        if (totalPoints == 0) return perMinuteHRV;

        int chunk = Math.max(1, totalPoints / 60);

        for (int i = 0; i < 60 && i * chunk < totalPoints; i++) {
            int start = i * chunk;
            int end = Math.min(totalPoints, start + chunk);

            float sum = 0f;

            for (int j = start; j < end; j++){
                sum += rmssdValues.get(j);
            }

            perMinuteHRV.add(sum / (end - start));
        }

        return perMinuteHRV;
    }

    /**
     * RMSSD formula:
     * sqrt( mean( (RR_i - RR_{i-1})² ) )
     */
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
