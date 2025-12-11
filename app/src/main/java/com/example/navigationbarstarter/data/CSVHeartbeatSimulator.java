package com.example.navigationbarstarter.data;

import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CSVHeartbeatSimulator {

    public interface HeartbeatCallback {
        void onHeartbeat(int bpm);
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Integer> bpmList = new ArrayList<>();
    private int currentIndex = 0;
    private boolean isRunning = false;
    private Runnable currentRunnable;

    public static Map<String, Integer> heartTime = new HashMap<>();

    public boolean loadCSV(InputStream csvInputStream) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(csvInputStream));
            String line;
            boolean firstLine = true;
            bpmList.clear();
            heartTime.clear();

            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                } //skip header
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    try {
                        String timestamp = parts[0].trim();
                        int bpm = Integer.parseInt(parts[1].trim());
                        bpmList.add(bpm);
                        heartTime.put(timestamp, bpm);
                    } catch (NumberFormatException e) {
                        //ignore malformed lines
                    }
                }
            }

            reader.close();
            return !bpmList.isEmpty();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Starts the simulation.
     * @param callback Interface to receive updates.
     * @param intervalMs Time between updates.
     * @param restartFromZero If true, starts from beginning of CSV. If false, continues from last index.
     */
    public void startSimulation(HeartbeatCallback callback, long intervalMs, boolean restartFromZero) {
        if (bpmList.isEmpty()) return;

        // If already running, stop first to avoid duplicates
        stopSimulation();

        if (restartFromZero) {
            currentIndex = 0;
        }

        isRunning = true;

        currentRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isRunning) return;

                if (currentIndex < bpmList.size()) {
                    int bpm = bpmList.get(currentIndex);
                    callback.onHeartbeat(bpm);

                    currentIndex++;
                    if (currentIndex >= bpmList.size()) currentIndex = 0; // Loop CSV
                }

                handler.postDelayed(this, intervalMs);
            }
        };

        handler.post(currentRunnable);
    }

    public void stopSimulation() {
        isRunning = false;
        if (currentRunnable != null) {
            handler.removeCallbacks(currentRunnable);
            currentRunnable = null;
        }
        handler.removeCallbacksAndMessages(null);
    }

    //Load new CSV with timestamp instead of BPM
    public static Map<Integer, List<String>> loadCsvTimestamp(InputStream csvInputStream) {
        Map<Integer, List<String>> sessions = new HashMap<>();

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(csvInputStream));
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {

                //Skip header
                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                String[] parts = line.split(",");
                if (parts.length < 2) continue;

                try {
                    int sessionId = Integer.parseInt(parts[0].trim());
                    String timestamp = parts[1].trim();

                    //Insert into the map
                    sessions.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(timestamp);
                } catch (Exception ignored) {
                    //Ignore malformed rows (must not be there in our case)
                }
            }

            reader.close();
            return sessions;

        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    public void reset() {
        stopSimulation();
        currentIndex = 0;
    }
}