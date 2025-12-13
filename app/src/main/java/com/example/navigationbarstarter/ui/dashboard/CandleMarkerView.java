package com.example.navigationbarstarter.ui.dashboard;

import android.content.Context;
import android.widget.TextView;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import java.util.List;
import static com.example.navigationbarstarter.ui.dashboard.FakeMLAlgorithm.*;

public class CandleMarkerView extends MarkerView {
    private TextView tvContent;
    private List<Float> bpm1Data;
    private List<Float> hrv1Data;
    private List<Float> bpm2Data;
    private List<Float> hrv2Data;
    private int baselineHR;
    private float baselineHRV;

    public CandleMarkerView(Context context, List<Float> bpm1, List<Float> hrv1,
                            List<Float> bpm2, List<Float> hrv2,
                            int baseline_hr, float baseline_hrv) {
        super(context, com.example.navigationbarstarter.R.layout.custom_marker_view);

        tvContent = findViewById(com.example.navigationbarstarter.R.id.tvContent);
        this.bpm1Data = bpm1;
        this.hrv1Data = hrv1;
        this.bpm2Data = bpm2;
        this.hrv2Data = hrv2;
        this.baselineHR = baseline_hr;
        this.baselineHRV = baseline_hrv;
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        if (e == null || !(e instanceof CandleEntry)) {
            return;
        }

        CandleEntry candleEntry = (CandleEntry) e;
        int sessionIndex = (int) candleEntry.getX(); // 0 for session 1, 1 for session 2

        // Select the correct session data
        List<Float> bpmData = (sessionIndex == 0) ? bpm1Data : bpm2Data;
        List<Float> hrvData = (sessionIndex == 0) ? hrv1Data : hrv2Data;

        if (bpmData.isEmpty() || hrvData.isEmpty()) {
            tvContent.setText("No data available");
            super.refreshContent(e, highlight);
            return;
        }

        // Calculate session statistics
        float avgBpm = 0;
        float avgHrv = 0;
        float minBpm = Float.MAX_VALUE;
        float maxBpm = Float.MIN_VALUE;
        float minHrv = Float.MAX_VALUE;
        float maxHrv = Float.MIN_VALUE;

        for (int i = 0; i < bpmData.size(); i++) {
            float bpm = bpmData.get(i);
            float hrv = hrvData.get(i);

            avgBpm += bpm;
            avgHrv += hrv;
            minBpm = Math.min(minBpm, bpm);
            maxBpm = Math.max(maxBpm, bpm);
            minHrv = Math.min(minHrv, hrv);
            maxHrv = Math.max(maxHrv, hrv);
        }

        avgBpm /= bpmData.size();
        avgHrv /= hrvData.size();

        // Analyze overall session stress
        int stressLevel = detectStressLevel((int)avgBpm, avgHrv, baselineHR, baselineHRV);

        String message = getSessionAnalysis(
                sessionIndex + 1,
                candleEntry,
                avgBpm,
                avgHrv,
                minBpm,
                maxBpm,
                minHrv,
                maxHrv,
                stressLevel
        );

        tvContent.setText(message);
        super.refreshContent(e, highlight);
    }

    private String getSessionAnalysis(int sessionNum, CandleEntry entry,
                                      float avgBpm, float avgHrv,
                                      float minBpm, float maxBpm,
                                      float minHrv, float maxHrv,
                                      int stressLevel) {

        StringBuilder message = new StringBuilder();
        message.append("📊 SESSION ").append(sessionNum).append("\n\n");

        // Candle information
        message.append("🔴 High: ").append(String.format("%.0f", entry.getHigh())).append(" BPM\n");
        message.append("🟢 Low: ").append(String.format("%.0f", entry.getLow())).append(" BPM\n");
        message.append("⚪ Open: ").append(String.format("%.0f", entry.getOpen())).append(" BPM\n");
        message.append("⚫ Close: ").append(String.format("%.0f", entry.getClose())).append(" BPM\n\n");

        // Average stats
        message.append("📈 Avg BPM: ").append(String.format("%.1f", avgBpm)).append("\n");
        message.append("💓 Avg HRV: ").append(String.format("%.1f", avgHrv)).append("\n\n");

        // Stress analysis
        message.append("🧠 ANALYSIS:\n");

        switch (stressLevel) {
            case STRESS_CRITICAL:
                message.append("⚠️ High stress detected!\n");
                message.append("High heart rate (").append(String.format("%.0f", maxBpm)).append(" BPM peak)\n");
                message.append("Low HRV (").append(String.format("%.1f", minHrv)).append(" min)\n");
                message.append("⚡ Action: Take a break immediately!\n");
                message.append("You experienced significant stress\nduring this session.");
                break;

            case STRESS_BREAK_RECOMMENDED:
                message.append("⚠️ Mental fatigue detected\n");
                message.append("Low HRV indicates reduced adaptability\n");
                message.append("💤 Action: Short break recommended\n");
                message.append("Your focus may have declined\nduring this session.");
                break;

            case STRESS_MONITOR:
                message.append("⚠️ High activity detected\n");
                message.append("Elevated heart rate (").append(String.format("%.0f", avgBpm)).append(" avg)\n");
                message.append("Good HRV maintained\n");
                message.append("👀 You were engaged and active,\nbut monitor your condition.");
                break;

            default:
                message.append("✅ Optimal performance!\n");
                message.append("Heart rate: Stable\n");
                message.append("HRV: Good\n");
                message.append("🎯 You maintained good focus\nand low stress throughout.");
                break;
        }

        return message.toString();
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2), -getHeight());
    }
}