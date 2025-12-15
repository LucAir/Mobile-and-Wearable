package com.example.navigationbarstarter.ui.dashboard;

import android.content.Context;
import android.widget.TextView;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import java.util.List;
import static com.example.navigationbarstarter.ui.dashboard.FakeMLAlgorithm.*;

public class StressMarkerView extends MarkerView {
    private TextView tvContent;
    private List<Float> bpmData;
    private List<Float> hrvData;
    private int baselineHR;
    private float baselineHRV;

    public StressMarkerView(Context context, List<Float> bpm, List<Float> hrv, int baseline_hr, float baseline_hrv) {
        super(context, com.example.navigationbarstarter.R.layout.custom_marker_view);

        tvContent = findViewById(com.example.navigationbarstarter.R.id.tvContent);
        this.bpmData = bpm;
        this.hrvData = hrv;
        this.baselineHR = baseline_hr;
        this.baselineHRV = baseline_hrv;
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        if (e == null) {
            return;
        }

        int index = (int) e.getX();

        //Validate index bounds
        if (index >= 0 && index < bpmData.size() && index < hrvData.size()) {
            float bpm = bpmData.get(index);
            float hrv = hrvData.get(index);

            int stressLevel = detectStressLevel((int)bpm, hrv, baselineHR, baselineHRV);

            String message = getStressMessage(stressLevel, (int)bpm, hrv, index);
            tvContent.setText(message);
        } else {
            tvContent.setText("Data not available");
        }

        super.refreshContent(e, highlight);
    }

    private String getStressMessage(int stressLevel, int bpm, float hrv, int minute) {
        switch (stressLevel) {
            case STRESS_CRITICAL:
                return "⚠️ CRITICAL STRESS\n" +
                        "Minute: " + (minute + 1) + "\n" +
                        "BPM: " + bpm + " | HRV: " + String.format("%.1f", hrv) + "\n\n" +
                        "High heart rate + Low HRV\n" +
                        "Your body is under significant stress.\n" +
                        "Take a break immediately!";

            case STRESS_BREAK_RECOMMENDED:
                return "⚠️ BREAK RECOMMENDED\n" +
                        "Minute: " + (minute + 1) + "\n" +
                        "BPM: " + bpm + " | HRV: " + String.format("%.1f", hrv) + "\n\n" +
                        "Low HRV detected\n" +
                        "Signs of mental fatigue.\n" +
                        "Consider taking a short break.";

            case STRESS_MONITOR:
                return "⚠️ MONITOR\n" +
                        "Minute: " + (minute + 1) + "\n" +
                        "BPM: " + bpm + " | HRV: " + String.format("%.1f", hrv) + "\n\n" +
                        "High heart rate detected\n" +
                        "You're active and engaged.\n" +
                        "Monitor your condition.";

            default:
                return "✓ Optimal State\n" +
                        "Minute: " + (minute + 1) + "\n" +
                        "BPM: " + bpm + " | HRV: " + String.format("%.1f", hrv);
        }
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2), -getHeight());
    }
}