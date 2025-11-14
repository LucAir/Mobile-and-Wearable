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

    public boolean loadCSV(InputStream csvInputStream) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(csvInputStream));
            String line;
            boolean firstLine = true;
            bpmList.clear();

            while ((line = reader.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; } // skip header
                String[] parts = line.split(",");
                int bpm = Integer.parseInt(parts[1].trim());
                bpmList.add(bpm);
            }

            reader.close();
            return !bpmList.isEmpty();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void startSimulation(HeartbeatCallback callback, long intervalMs) {
        if (bpmList.isEmpty()) return;
        isRunning = true;
        currentIndex = 0;

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (!isRunning) return;

                int bpm = bpmList.get(currentIndex);
                callback.onHeartbeat(bpm);

                currentIndex++;
                if (currentIndex >= bpmList.size()) currentIndex = 0;

                handler.postDelayed(this, intervalMs);
            }
        };

        handler.post(runnable);
    }

    public void stopSimulation() {
        isRunning = false;
        handler.removeCallbacksAndMessages(null);
    }
}
