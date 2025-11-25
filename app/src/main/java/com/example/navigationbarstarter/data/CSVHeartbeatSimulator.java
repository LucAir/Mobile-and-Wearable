package com.example.navigationbarstarter.data;

import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CSVHeartbeatSimulator {

    public interface HeartbeatCallback {
        void onHeartbeat(int bpm);
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Integer> bpmList = new ArrayList<>();
    private int currentIndex = 0;
    private boolean isRunning = false;
    private Runnable currentRunnable;

    public boolean loadCSV(InputStream csvInputStream) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(csvInputStream));
            String line;
            boolean firstLine = true;
            bpmList.clear();

            while ((line = reader.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; } // skip header
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    try {
                        int bpm = Integer.parseInt(parts[1].trim());
                        bpmList.add(bpm);
                    } catch (NumberFormatException e) {
                        // ignore malformed lines
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

    public void reset() {
        stopSimulation();
        currentIndex = 0;
    }
}