package com.example.navigationbarstarter.ui.dashboard;

import android.content.Context;
import android.widget.TextView;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import static com.example.navigationbarstarter.ui.dashboard.FakeMLAlgorithm.*;

/**
 * Custom marker view that shows detailed statistics for each box plot session
 */
public class CandleMarkerView extends MarkerView {

    private TextView tvContent;
    private List<Float> bpm1;
    private List<Float> bpm2;
    private List<Float> hrv1;
    private List<Float> hrv2;
    private int baselineHR;
    private float baselineHRV;

    public CandleMarkerView(Context context, List<Float> bpm1, List<Float> bpm2,
                             List<Float> hrv1, List<Float> hrv2,
                             int baseline_hr, float baseline_hrv) {
        super(context, com.example.navigationbarstarter.R.layout.custom_marker_view);

        this.bpm1 = bpm1;
        this.bpm2 = bpm2;
        this.hrv1 = hrv1;
        this.hrv2 = hrv2;
        this.baselineHR = baseline_hr;
        this.baselineHRV = baseline_hrv;

        tvContent = findViewById(com.example.navigationbarstarter.R.id.tvContent);
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        int sessionIndex = (int) e.getX(); // 0 = Session 1, 1 = Session 2

        List<Float> bpmData = (sessionIndex == 0) ? bpm1 : bpm2;
        List<Float> hrvData = (sessionIndex == 0) ? hrv1 : hrv2;

        if (bpmData == null || bpmData.isEmpty() || hrvData == null || hrvData.isEmpty()) {
            tvContent.setText("No data available");
            super.refreshContent(e, highlight);
            return;
        }

        // Calculate statistics
        SessionStats stats = calculateSessionStats(bpmData, hrvData);

        // Build marker content
        String content = buildMarkerContent(sessionIndex + 1, stats);
        tvContent.setText(content);

        super.refreshContent(e, highlight);
    }

    /**
     * Calculate all session statistics including stress levels
     */
    private SessionStats calculateSessionStats(List<Float> bpmData, List<Float> hrvData) {
        SessionStats stats = new SessionStats();

        // Basic BPM statistics
        stats.bpmMin = Collections.min(bpmData);
        stats.bpmMax = Collections.max(bpmData);
        stats.bpmAvg = calculateAverage(bpmData);
        stats.bpmStdDev = calculateStdDev(bpmData, stats.bpmAvg);
        stats.sampleCount = bpmData.size();

        // Basic HRV statistics
        stats.hrvMin = Collections.min(hrvData);
        stats.hrvMax = Collections.max(hrvData);
        stats.hrvAvg = calculateAverage(hrvData);

        // Count outliers (>10 BPM from average)
        stats.outlierCount = 0;
        for (float bpm : bpmData) {
            if (Math.abs(bpm - stats.bpmAvg) > 10f) {
                stats.outlierCount++;
            }
        }

        // Analyze stress levels for each sample
        stats.criticalCount = 0;
        stats.warningCount = 0;
        stats.optimalCount = 0;

        for (int i = 0; i < Math.min(bpmData.size(), hrvData.size()); i++) {
            int stressLevel = detectStressLevel(
                    (int) bpmData.get(i).floatValue(),
                    hrvData.get(i),
                    baselineHR,
                    baselineHRV
            );

            switch (stressLevel) {
                case STRESS_CRITICAL:
                    stats.criticalCount++;
                    break;
                case STRESS_MONITOR:
                case STRESS_BREAK_RECOMMENDED:
                    stats.warningCount++;
                    break;
                case STRESS_OPTIMAL_STATE:
                    stats.optimalCount++;
                    break;
            }
        }

        return stats;
    }

    /**
     * Build the marker content string
     */
    private String buildMarkerContent(int sessionNum, SessionStats stats) {
        StringBuilder sb = new StringBuilder();

        //Header
        sb.append(String.format(Locale.getDefault(), "━━━ SESSION %d ━━━\n\n", sessionNum));

        //BPM Statistics
        sb.append("📊 BPM STATISTICS\n");
        sb.append(String.format(Locale.getDefault(), "  Average: %.1f\n", stats.bpmAvg));

        //Stress Analysis
        sb.append("🧠 STRESS ANALYSIS\n");
        sb.append(String.format(Locale.getDefault(), "  ⚠️ Warnings: %d\n", stats.warningCount));
        sb.append(String.format(Locale.getDefault(), "  🔴 Critical: %d\n", stats.criticalCount));

        //Explanation of warnings/critical
        sb.append("ℹ️ WHAT THEY MEAN:\n");
        if (stats.criticalCount > 0) {
            sb.append("  🔴 Critical: High HR + Low HRV\n");
            sb.append("     → Stressed, break needed\n");
        }
        if (stats.warningCount > 0) {
            sb.append("  ⚠️ Warnings:\n");
            sb.append("     • High HR + Good HRV\n");
            sb.append("       → Active/engaged, monitor\n");
            sb.append("     • Low HR + Low HRV\n");
            sb.append("       → Mental fatigue, rest needed\n");
        }

        return sb.toString();
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2f), -getHeight() - 10);
    }

    /**
     * Helper class to hold session statistics
     */
    private static class SessionStats {
        float bpmMin, bpmMax, bpmAvg, bpmStdDev;
        float hrvMin, hrvMax, hrvAvg;
        int sampleCount;
        int outlierCount;
        int criticalCount;
        int warningCount;
        int optimalCount;
    }

    private float calculateAverage(List<Float> data) {
        if (data == null || data.isEmpty()) return 0f;
        float sum = 0;
        for (float value : data) {
            sum += value;
        }
        return sum / data.size();
    }

    private float calculateStdDev(List<Float> data, float mean) {
        if (data == null || data.isEmpty()) return 0f;
        float sumSquaredDiff = 0;
        for (float value : data) {
            float diff = value - mean;
            sumSquaredDiff += diff * diff;
        }
        return (float) Math.sqrt(sumSquaredDiff / data.size());
    }
}