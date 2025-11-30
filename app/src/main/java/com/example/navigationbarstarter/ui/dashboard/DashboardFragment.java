package com.example.navigationbarstarter.ui.dashboard;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.Highlights;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.navigationbarstarter.data.CSVHeartbeatSimulator;
import com.example.navigationbarstarter.database.AppDatabase;
import com.example.navigationbarstarter.database.session.SessionData;
import com.example.navigationbarstarter.databinding.FragmentDashboardBinding;
import com.github.mikephil.charting.charts.CandleStickChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.CandleData;
import com.github.mikephil.charting.data.CandleDataSet;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DashboardFragment extends Fragment {

    private AppDatabase appDatabase;
    private Executor executor;
    private FragmentDashboardBinding binding;

    private long userId;

    private CandleStickChart candleChart;

    private LineChart heartRateChart;

    private TreeMap<String, Integer> allHeartData;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        DashboardViewModel dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);

        appDatabase = AppDatabase.getInstance(getContext());
        executor = Executors.newSingleThreadExecutor();

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        //Binding elements
        candleChart = binding.candleChart;
        heartRateChart = binding.heartRateChart;

        getUserIdFromSharedPreferences();

        loadDataHeartBeat();

        return root;
    }

    //Used to retrieve UserID and so collect all the session it has in order to create charts
    private void getUserIdFromSharedPreferences() {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserPrefs", MODE_PRIVATE);
        this.userId = sharedPreferences.getLong("UserId", userId);
    }


    //TODO: change to a better hover
    private void createHeartRateChartWithMinutes(TreeMap<String, Integer> allHeartData) {

        //Statistics
        int min = 0;
        int max = 0;
        int sum = 0;
        double avg = 0.0;

        //Get last 60 data points (or all if less than 60)
        List<Map.Entry<String, Integer>> allEntries = new ArrayList<>(allHeartData.entrySet());
        int totalPoints = allEntries.size();
        int startIndex = Math.max(0, totalPoints - 60);

        List<Entry> entries = new ArrayList<>();

        //Create entries with X = minutes (0-60)
        for (int i = startIndex; i < totalPoints; i++) {
            int minute = i - startIndex; // 0, 1, 2, ... up to 59
            int bpm = allEntries.get(i).getValue();
            entries.add(new Entry(minute, bpm));
            if (min > allEntries.get(i).getValue()) {
                min = allEntries.get(i).getValue();
            }
            if (max < allEntries.get(i).getValue()) {
                max = allEntries.get(i).getValue();
            }
            sum = sum + allEntries.get(i).getValue();
        }

        avg = sum / 60;

        if (entries.isEmpty()) {
            heartRateChart.clear();
            heartRateChart.invalidate();
            return;
        }

        //Create dataset
        LineDataSet dataSet = new LineDataSet(entries, "Heart Rate (BPM)");

        //Styling for heartbeat line
        dataSet.setColor(Color.rgb(255, 69, 58)); //Red color for heart
        dataSet.setCircleColor(Color.rgb(255, 69, 58));
        dataSet.setCircleRadius(2f);
        dataSet.setDrawCircleHole(false);
        dataSet.setLineWidth(2f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); //Smooth curve
        dataSet.setCubicIntensity(0.2f);

        //Add gradient fill (Areas under the line
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.rgb(255, 69, 58));
        dataSet.setFillAlpha(50);

        LineData lineData = new LineData(dataSet);

        //Configure chart
        heartRateChart.setData(lineData);
        heartRateChart.getDescription().setEnabled(false);
        heartRateChart.setDrawGridBackground(false);
        heartRateChart.setTouchEnabled(true);
        heartRateChart.setDragEnabled(true);
        heartRateChart.setScaleEnabled(true);
        heartRateChart.setPinchZoom(true);

        //Configure X axis (minutes)
        XAxis xAxis = heartRateChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.LTGRAY);
        xAxis.setGranularity(1f);
        xAxis.setTextSize(10f);
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(59f); // Force 0-59 range
        xAxis.setLabelCount(13, false); // Show 0, 5, 10, 15, 20... 60
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                // Just show the minute number
                return String.valueOf((int) value);
            }
        });

        //Configure Y axis (BPM)
        YAxis leftAxis = heartRateChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.LTGRAY);
        leftAxis.setTextSize(10f);

        //Find min and max BPM in current data
        float minBpm = 50f;
        float maxBpm = 0f;

        for (Entry entry : entries) {
            if (entry.getY() > maxBpm) {
                maxBpm = entry.getY() + 10;
            }
            if (entry.getY() < minBpm) {
                minBpm = entry.getY() - 5;
            }
        }

        leftAxis.setAxisMinimum(minBpm);
        leftAxis.setAxisMaximum(maxBpm);
        leftAxis.setLabelCount(8, false);

        YAxis rightAxis = heartRateChart.getAxisRight();
        rightAxis.setEnabled(false);

        //Adding median value bpm as a line
        LimitLine medianLine = new LimitLine((float) avg, "Median: " + (int)avg);
        medianLine.setLineColor(Color.BLUE);
        medianLine.setLineWidth(2f);
        medianLine.enableDashedLine(10f, 10f, 0f);
        leftAxis.addLimitLine(medianLine);

        //Add legend configuration
        heartRateChart.getLegend().setTextSize(12f);
        heartRateChart.getLegend().setXOffset(0f);
        heartRateChart.getLegend().setYOffset(0f);

        //Animate chart
        heartRateChart.animateX(800);
        heartRateChart.invalidate();

        //Add a listener to the chart for the over
        int finalMax = max;
        int finalMin = min;
        double finalAvg = avg;
        heartRateChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                //When clicking on a point
                int minute = (int) e.getX();
                int bpm = (int) e.getY();
                Toast.makeText(getContext(), "Minute: " + minute + ", BPM: " + bpm, Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onNothingSelected() {
                //When clicking on the area (not a specific point)
                Toast.makeText(getContext(), "Max: " + finalMax + " BPM, Min: " + finalMin + " BPM, Avg: " + (int) finalAvg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Create candlestick chart where each candle represents a session
     * High = max BPM, Low = min BPM, Open/Close = start/end BPM
     */
    private void createCandleChart(TreeMap<String, Integer> allHeartData) {

        if (allHeartData == null || allHeartData.isEmpty()) {
            return;
        }

        //Get last 120 data points (or all if less than 120)
        List<Map.Entry<String, Integer>> allEntries = new ArrayList<>(allHeartData.entrySet());
        int totalPoints = allEntries.size();
        int startIndex = Math.max(0, totalPoints - 120);

        //Split into 2 sessions of 60 points each
        List<CandleEntry> candleEntries = new ArrayList<>();

        for (int sessionNum = 0; sessionNum < 2; sessionNum++) {
            int sessionStartIndex = startIndex + (sessionNum * 60);
            int sessionEndIndex = Math.min(sessionStartIndex + 60, totalPoints);

            if (sessionStartIndex >= totalPoints) break;

            //Calculate stats for this session
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;
            int sum = 0;
            int count = 0;
            int firstBpm = 0;
            int lastBpm = 0;

            for (int i = sessionStartIndex; i < sessionEndIndex; i++) {
                int bpm = allEntries.get(i).getValue();

                if (count == 0) firstBpm = bpm;
                lastBpm = bpm;

                min = Math.min(min, bpm);
                max = Math.max(max, bpm);
                sum += bpm;
                count++;
            }

            float avg = count > 0 ? (float) sum / count : 0;

            //CandleEntry(x, high, low, open, close)
            candleEntries.add(new CandleEntry(
                    sessionNum,     //X position (0 or 1)
                    max,           //High
                    min,           //Low
                    firstBpm,      //Open (first BPM)
                    lastBpm        //Close (last BPM)
            ));
        }

        CandleDataSet dataSet = new CandleDataSet(candleEntries, "Sessions");

        //Styling
        dataSet.setDecreasingColor(Color.rgb(255, 69, 58)); //Red for decreasing
        dataSet.setDecreasingPaintStyle(Paint.Style.FILL);
        dataSet.setIncreasingColor(Color.rgb(52, 199, 89)); //Green for increasing
        dataSet.setIncreasingPaintStyle(Paint.Style.FILL);
        dataSet.setShadowColor(Color.DKGRAY);
        dataSet.setShadowWidth(2f);
        dataSet.setDrawValues(false);
        dataSet.setNeutralColor(Color.BLUE);

        CandleData candleData = new CandleData(dataSet);

        //Configure chart
        candleChart.setData(candleData);
        candleChart.getDescription().setEnabled(false);
        candleChart.setDrawGridBackground(false);
        candleChart.setTouchEnabled(true);
        candleChart.setDragEnabled(true);
        candleChart.setScaleEnabled(true);
        candleChart.setPinchZoom(true);

        //X axis
        XAxis xAxisCandle = candleChart.getXAxis();
        xAxisCandle.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxisCandle.setDrawGridLines(true);
        xAxisCandle.setGridColor(Color.LTGRAY);
        xAxisCandle.setGranularity(1f);
        xAxisCandle.setTextSize(10f);
        xAxisCandle.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return "Session " + ((int) value + 1); // Session 1, Session 2
            }
        });

        //Y axis
        YAxis leftAxisCandle = candleChart.getAxisLeft();
        leftAxisCandle.setDrawGridLines(true);
        leftAxisCandle.setGridColor(Color.LTGRAY);
        leftAxisCandle.setTextSize(10f);
        leftAxisCandle.setAxisMinimum(50f);
        leftAxisCandle.setAxisMaximum(180f);

        YAxis rightAxisCandle = candleChart.getAxisRight();
        rightAxisCandle.setEnabled(false);

        candleChart.getLegend().setTextSize(12f);
        candleChart.animateX(1000);
        candleChart.invalidate();
    }

    private void loadDataHeartBeat() {
        Map<String, Integer> heartTime = CSVHeartbeatSimulator.heartTime;
        if (heartTime == null || heartTime.isEmpty()) {
            return;
        }

        //Sort timestamp for correct display
        TreeMap<String, Integer> sortedData = new TreeMap<>(heartTime);

        createHeartRateChartWithMinutes(sortedData);
        createCandleChart(sortedData);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}