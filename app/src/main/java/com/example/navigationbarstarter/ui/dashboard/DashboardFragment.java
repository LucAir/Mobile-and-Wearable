package com.example.navigationbarstarter.ui.dashboard;

import static android.content.Context.MODE_PRIVATE;
import android.util.Log;
import static com.example.navigationbarstarter.data.CSVHeartbeatSimulator.loadCsvTimestamp;
import static com.example.navigationbarstarter.data.HeartRateVariability.computeBPM;
import static com.example.navigationbarstarter.data.HeartRateVariability.computeHRV;
import static com.example.navigationbarstarter.ui.dashboard.FakeMLAlgorithm.STRESS_BREAK_RECOMMENDED;
import static com.example.navigationbarstarter.ui.dashboard.FakeMLAlgorithm.STRESS_CRITICAL;
import static com.example.navigationbarstarter.ui.dashboard.FakeMLAlgorithm.STRESS_MONITOR;
import static com.example.navigationbarstarter.ui.dashboard.FakeMLAlgorithm.detectStressLevel;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.navigationbarstarter.R;
import com.example.navigationbarstarter.database.AppDatabase;
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
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DashboardFragment extends Fragment {

    private AppDatabase appDatabase;
    private Executor executor;
    private FragmentDashboardBinding binding;

    private long userId;

    private CandleStickChart candleChart;

    private LineChart heartRateChart;

    private TabLayout tab;

    private TextView txtFeature;

    //Used to store baseline values
    private float userBaseline_hr;
    private float userBaseline_hrv;

    private Map<Integer, List<String>> sessions;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        DashboardViewModel dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);

        appDatabase = AppDatabase.getInstance(getContext());
        executor = Executors.newSingleThreadExecutor();

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        //Binding elements
        candleChart = binding.candleStickChart;
        heartRateChart = binding.lineChart;
        tab = binding.tabLayout;
        txtFeature = binding.txtFeature;

        //Initial visibility on startup
        heartRateChart.setVisibility(View.VISIBLE);
        candleChart.setVisibility(View.INVISIBLE);
        txtFeature.setVisibility(View.INVISIBLE);

        //Initialize hashmap
        this.sessions = loadCsvTimestamp(requireContext().getResources().openRawResource(R.raw.two_sessions));
        Log.d("CSV_DEBUG", "Session1 size:" + (sessions != null ? sessions.size() : "null"));

        getUserIdFromSharedPreferences();

        dashboardViewModel.getBaselineHr(userId);
        dashboardViewModel.getBaselineHrv(userId);

        setUpListener();

        //Checking if live variables has changed
        dashboardViewModel.getUserBaselineHr().observe(getViewLifecycleOwner(), baselineHr -> {
            if (baselineHr != 0) {
                this.userBaseline_hr = baselineHr;

                //Checking if live variables has changed
                dashboardViewModel.getUserBaselineHrv().observe(getViewLifecycleOwner(), baselineHrv -> {
                    if (baselineHrv != 0) {
                        this.userBaseline_hrv = baselineHrv;
                        createHeartRateChartWithMinutes();
                        createCandleChart();
                    }
                });
            }
        });

        return root;
    }

    //Used to retrieve UserID and so collect all the session it has in order to create charts
    private void getUserIdFromSharedPreferences() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", MODE_PRIVATE);
        this.userId = sharedPreferences.getLong("userId", -1);
        Log.d("DASH_DEBUG", "Loaded userId=" + userId);
    }

    private void setUpListener() {
        tab.selectTab(tab.getTabAt(0));
        tab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab t) {
                switch (t.getPosition()) {
                    case 0:
                        heartRateChart.setVisibility(View.VISIBLE);
                        candleChart.setVisibility(View.INVISIBLE);
                        txtFeature.setVisibility(View.INVISIBLE);
                        break;

                    case 1:
                        heartRateChart.setVisibility(View.INVISIBLE);
                        candleChart.setVisibility(View.VISIBLE);
                        txtFeature.setVisibility(View.INVISIBLE);
                        break;

                    case 2:
                        heartRateChart.setVisibility(View.INVISIBLE);
                        candleChart.setVisibility(View.INVISIBLE);
                        txtFeature.setVisibility(View.VISIBLE);
                        break;
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab t) {}
            @Override public void onTabReselected(TabLayout.Tab t) {}
        });
    }

    private void createHeartRateChartWithMinutes() {
        List<String> session1 = sessions.get(1);
        List<Float> bpm = new ArrayList<>();
        List<Float> hrv = new ArrayList<>();
        if (session1 != null) {
            //Compute BPM and HRV per minute (have same points)
            bpm = computeBPM(session1);
            hrv = computeHRV(session1);
        }

        //Ensure same size (JUST TO BE SURE)
        int size = Math.min(bpm.size(), hrv.size());

        //Build entries from the single source of truth
        List<Entry> greenEntries = new ArrayList<>(); //Contains all BPM points (line)
        List<Entry> yellowEntries = new ArrayList<>(); //Contains only warnings point (point)
        List<Entry> redEntries = new ArrayList<>(); //Contains only critical point (point)

        float sum = 0;
        int max = Integer.MIN_VALUE;

        for (int i = 0; i < size; i++) {
            float b = bpm.get(i);
            float h = hrv.get(i);
            greenEntries.add(new Entry(i, b)); //ALL points go to green

            sum += b;
            if (b > max) max = (int) b;

            int stressLevel = detectStressLevel((int)b, h, (int) userBaseline_hr, userBaseline_hrv);
            if (stressLevel == STRESS_CRITICAL) {
                redEntries.add(new Entry(i, b));  //Same x and y
            } else if (stressLevel == STRESS_BREAK_RECOMMENDED || stressLevel == STRESS_MONITOR) {
                yellowEntries.add(new Entry(i, b)); //Same x and y
            }
        }

        float avg = sum / size;

        //Chart settings
        heartRateChart.setDrawGridBackground(false);
        heartRateChart.getDescription().setEnabled(false);
        heartRateChart.getLegend().setEnabled(true);

        //Enable touch and highlighting
        heartRateChart.setTouchEnabled(true);
        heartRateChart.setDragEnabled(true);
        heartRateChart.setScaleEnabled(true);
        heartRateChart.setPinchZoom(true);
        heartRateChart.setHighlightPerTapEnabled(true);
        heartRateChart.setHighlightPerDragEnabled(false);
        heartRateChart.setMaxHighlightDistance(50f); //Only highlight if within 50 pixels

        //Configure Y axis (left) - only left axis visible
        YAxis leftAxis = heartRateChart.getAxisLeft();
        leftAxis.setAxisMinimum(40f);  //Start from 40 BPM
        leftAxis.setEnabled(true);

        YAxis rightAxis = heartRateChart.getAxisRight();
        rightAxis.setEnabled(false);  //Hide right Y axis

        //Configure X axis (bottom) - only bottom axis visible
        XAxis xAxis = heartRateChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);  //Show X axis at bottom
        xAxis.setEnabled(true);

        //GREEN dataset: full line
        LineDataSet greenSet = new LineDataSet(greenEntries, "Heart Rate (BPM)");
        greenSet.setColor(Color.GREEN);
        greenSet.setLineWidth(3f);
        greenSet.setMode(LineDataSet.Mode.LINEAR);
        greenSet.setDrawFilled(true);  //Enable fill
        greenSet.setFillColor(Color.GREEN);
        greenSet.setFillAlpha(65);
        greenSet.setDrawValues(false);
        greenSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        greenSet.setDrawCircles(false);
        greenSet.setDrawCircleHole(false);
        greenSet.setHighlightEnabled(false);

        //YELLOW overlay: only circles
        LineDataSet yellowSet = new LineDataSet(yellowEntries, "Warning");
        yellowSet.setColor(Color.TRANSPARENT);
        yellowSet.setDrawValues(false);
        yellowSet.setDrawCircles(true);
        yellowSet.setCircleColor(Color.YELLOW);
        yellowSet.setCircleRadius(6f);
        yellowSet.setDrawCircleHole(false);
        yellowSet.setLineWidth(0f);
        yellowSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        yellowSet.setHighlightEnabled(true);
        yellowSet.setDrawHorizontalHighlightIndicator(false);
        yellowSet.setDrawVerticalHighlightIndicator(false);

        //RED overlay: only circles
        LineDataSet redSet = new LineDataSet(redEntries, "Critical");
        redSet.setColor(Color.TRANSPARENT);
        redSet.setDrawValues(false);
        redSet.setDrawCircles(true);
        redSet.setCircleColor(Color.RED);
        redSet.setCircleRadius(6f);
        redSet.setDrawCircleHole(false);
        redSet.setLineWidth(0f);
        redSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        redSet.setHighlightEnabled(true);
        redSet.setDrawHorizontalHighlightIndicator(false);
        redSet.setDrawVerticalHighlightIndicator(false);

        //ADD MEDIAN LINE
        LimitLine medianLine = new LimitLine(avg, "Median: " + (int) avg);
        medianLine.setLineColor(Color.BLUE);
        medianLine.setLineWidth(2f);
        medianLine.enableDashedLine(10f, 10f, 0f);
        leftAxis.addLimitLine(medianLine);

        //Add green first, overlay sets after so they render on top
        LineData lineData = new LineData();
        lineData.addDataSet(greenSet);
        lineData.addDataSet(yellowSet);
        lineData.addDataSet(redSet);

        heartRateChart.setData(lineData);

        //Set custom marker for showing details on tap
        heartRateChart.setMarker(new StressMarkerView(getContext(), bpm, hrv, (int)userBaseline_hr, userBaseline_hrv));

        //Custom listener to only show marker on yellow/red points
        heartRateChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                //Only show marker if it's a yellow or red point (dataset index 1 or 2)
                if (h.getDataSetIndex() == 1 || h.getDataSetIndex() == 2) {
                    heartRateChart.highlightValue(h);
                } else {
                    heartRateChart.highlightValue(null); // Clear highlight
                }
            }

            @Override
            public void onNothingSelected() {
                // Do nothing
            }
        });

        heartRateChart.animateX(800);
        heartRateChart.invalidate();
    }

    /**
     * Create candlestick chart where each candle represents a session
     * High = max BPM, Low = min BPM, Open/Close = start/end BPM
     */
    private void createCandleChart() {

        /**
         * Here I wanted to check if two list contains data. I don't do nested if, because if the user
         * has only one session recorded, I want him to be able to see the candleChart
         */

        List<String> session1 = sessions.get(1);
        List<String> session2 = sessions.get(2);
        List<Float> bpm1 = new ArrayList<>();
        List<Float> hrv1 = new ArrayList<>();
        List<Float> bpm2 = new ArrayList<>();
        List<Float> hrv2 = new ArrayList<>();

        //Check if session1 contains data
        if(session1 != null && !session1.isEmpty()) {
            bpm1 = computeBPM(session1);
            hrv1 = computeHRV(session1);
        }

        //Check if session 2 contains data
        if(session2 != null && !session2.isEmpty()) {
            bpm2 = computeBPM(session2);
            hrv2 = computeHRV(session2);
        }

        if (session1.isEmpty() && session2.isEmpty()) {
            return;
        }

        List<CandleEntry> candleEntries = new ArrayList<>();

        //Compute entries:
        // Calculate stats for session 1 (if exists)
        if (!bpm1.isEmpty()) {
            float min = Float.MAX_VALUE;
            float max = Float.MIN_VALUE;
            float sum = 0;
            float firstBpm = bpm1.get(0);
            float lastBpm = bpm1.get(bpm1.size() - 1);

            for (float b : bpm1) {
                min = Math.min(min, b);
                max = Math.max(max, b);
                sum += b;
            }

            float avg = sum / bpm1.size();

            //CandleEntry(x, high, low, open, close)
            candleEntries.add(new CandleEntry(
                    0,         //X position for session 1
                    max,       //High
                    min,       //Low
                    firstBpm,  //Open (first BPM)
                    lastBpm    //Close (last BPM)
            ));
        }

        //Calculate stats for session 2 (if exists)
        if (!bpm2.isEmpty()) {
            float min = Float.MAX_VALUE;
            float max = Float.MIN_VALUE;
            float sum = 0;
            float firstBpm = bpm2.get(0);
            float lastBpm = bpm2.get(bpm2.size() - 1);

            for (float b : bpm2) {
                min = Math.min(min, b);
                max = Math.max(max, b);
                sum += b;
            }

            float avg = sum / bpm2.size();

            //CandleEntry(x, high, low, open, close)
            candleEntries.add(new CandleEntry(
                    1,
                    max,
                    min,
                    firstBpm,
                    lastBpm
            ));
        }

        CandleDataSet dataSet = new CandleDataSet(candleEntries, "Sessions");

        //Styling
        dataSet.setDecreasingColor(Color.rgb(255, 69, 58));
        dataSet.setDecreasingPaintStyle(Paint.Style.FILL);
        dataSet.setIncreasingColor(Color.rgb(52, 199, 89));
        dataSet.setIncreasingPaintStyle(Paint.Style.FILL);
        dataSet.setShadowColor(Color.DKGRAY);
        dataSet.setShadowWidth(2f);
        dataSet.setDrawValues(false);
        dataSet.setNeutralColor(Color.BLUE);

        //Enable highlighting for interactive features
        dataSet.setHighlightEnabled(true);
        dataSet.setHighLightColor(Color.WHITE);

        CandleData candleData = new CandleData(dataSet);

        //Configure chart
        candleChart.setData(candleData);
        candleChart.getDescription().setEnabled(false);
        candleChart.setDrawGridBackground(false);
        candleChart.setTouchEnabled(true);
        candleChart.setDragEnabled(true);
        candleChart.setScaleEnabled(true);
        candleChart.setPinchZoom(true);
        candleChart.setHighlightPerTapEnabled(true);
        candleChart.setHighlightPerDragEnabled(false);

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
                return "Session " + ((int) value + 1); //Session 1, Session 2
            }
        });

        //Y axis
        YAxis leftAxisCandle = candleChart.getAxisLeft();
        leftAxisCandle.setDrawGridLines(true);
        leftAxisCandle.setGridColor(Color.LTGRAY);
        leftAxisCandle.setTextSize(10f);
        leftAxisCandle.setAxisMinimum(40f); //Start from 40 BPM like the line chart

        //Find max in current data
        float maxBpm = 40f;
        for (CandleEntry entry : candleEntries) {
            if (entry.getHigh() > maxBpm) {
                maxBpm = entry.getHigh() + 10;
            }
        }
        leftAxisCandle.setAxisMaximum(maxBpm);

        YAxis rightAxisCandle = candleChart.getAxisRight();
        rightAxisCandle.setEnabled(false);

        //Set custom marker for candle chart
        candleChart.setMarker(new CandleMarkerView(getContext(), bpm1, hrv1, bpm2, hrv2, (int)userBaseline_hr, userBaseline_hrv));

        candleChart.getLegend().setTextSize(12f);
        candleChart.animateX(1000);
        candleChart.invalidate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}