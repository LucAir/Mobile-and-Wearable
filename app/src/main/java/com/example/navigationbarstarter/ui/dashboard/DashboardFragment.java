package com.example.navigationbarstarter.ui.dashboard;

import static android.content.Context.MODE_PRIVATE;

import android.graphics.Paint;
import android.util.Log;
import static com.example.navigationbarstarter.data.HeartRateVariability.computeBPM;
import static com.example.navigationbarstarter.data.HeartRateVariability.computeHRV;
import static com.example.navigationbarstarter.ui.dashboard.FakeMLAlgorithm.STRESS_BREAK_RECOMMENDED;
import static com.example.navigationbarstarter.ui.dashboard.FakeMLAlgorithm.STRESS_CRITICAL;
import static com.example.navigationbarstarter.ui.dashboard.FakeMLAlgorithm.STRESS_MONITOR;
import static com.example.navigationbarstarter.ui.dashboard.FakeMLAlgorithm.detectStressLevel;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.navigationbarstarter.R;
import com.example.navigationbarstarter.database.AppDatabase;
import com.example.navigationbarstarter.database.session.SessionViewModel;
import com.example.navigationbarstarter.databinding.FragmentDashboardBinding;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.CandleData;
import com.github.mikephil.charting.data.CandleDataSet;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DashboardFragment extends Fragment {

    private AppDatabase appDatabase;
    private Executor executor;
    private FragmentDashboardBinding binding;
    private long userId;
    private CombinedChart combinedChart;
    private LineChart heartRateChart;
    private TabLayout tab;
    private TextView txtFeature;

    //Used to store baseline values
    private float userBaseline_hr;
    private float userBaseline_hrv;

    //Ordering and retrieving session values
    private boolean isDescending = true;
    private SessionViewModel sessionViewModel;
    private Button btnOrderToggle;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        sessionViewModel =
                new ViewModelProvider(this).get(SessionViewModel.class);

        appDatabase = AppDatabase.getInstance(getContext());
        executor = Executors.newSingleThreadExecutor();

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        //Binding elements
        combinedChart = binding.combinedChart;
        heartRateChart = binding.lineChart;
        tab = binding.tabLayout;
        txtFeature = binding.txtFeature;
        btnOrderToggle = binding.btnOrderToggle;
        btnOrderToggle.setVisibility(View.GONE);

        //Initial visibility on startup
        heartRateChart.setVisibility(View.VISIBLE);
        combinedChart.setVisibility(View.INVISIBLE);
        txtFeature.setVisibility(View.INVISIBLE);

        //Get user ID
        getUserIdFromSharedPreferences();

        //Load last 2 session for boxplot
        sessionViewModel.loadComparedSessions(userId);

        setUpListener(sessionViewModel);

        //Observe baseline HR and HRV
        observeBaselines();

        //Observe sessions for both line chart (last session) and boxplot (last 2 sessions)
        sessionViewModel.getComparedSessions().observe(getViewLifecycleOwner(), allSessions -> {
            if (allSessions == null || allSessions.isEmpty()) {
                Log.d("DashboardFragment", "allSessions is null or empty");
                return;
            }

            Log.d("DashboardFragment", "Received " + allSessions.size() + " sessions");

            //Initialize chart after collecting baseline values -> we can display information
            if(userBaseline_hr != 0 && userBaseline_hrv != 0) {

                //Render linechart
                createHeartRateChartWithMinutes(allSessions.get(0));

                //Boxplot: last 2 sessions
                if (allSessions.size() >= 2) {
                   createCandleChart(allSessions.get(0), allSessions.get(1));
                } else if (allSessions.size() == 1) {
                   createCandleChart(allSessions.get(0), null);
                }
            }
            Log.d("DashboardFragment",
                    "Sessions received: " + allSessions.size() +
                            ", last size=" + allSessions.get(allSessions.size() - 1).size()
            );
        });

        return root;
    }

    //Used to retrieve UserID and so collect all the session it has in order to create charts
    private void getUserIdFromSharedPreferences() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", MODE_PRIVATE);
        this.userId = sharedPreferences.getLong("userId", -1);
    }

    //Setting up listener for tabs and button
    private void setUpListener(SessionViewModel sessionViewModel) {
        tab.selectTab(tab.getTabAt(0));
        tab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab t) {
                switch (t.getPosition()) {
                    case 0:
                        heartRateChart.setVisibility(View.VISIBLE);
                        combinedChart.setVisibility(View.INVISIBLE);
                        txtFeature.setVisibility(View.INVISIBLE);
                        btnOrderToggle.setVisibility(View.GONE);
                        break;

                    case 1:
                        heartRateChart.setVisibility(View.INVISIBLE);
                        combinedChart.setVisibility(View.VISIBLE);
                        txtFeature.setVisibility(View.INVISIBLE);
                        btnOrderToggle.setVisibility(View.VISIBLE);
                        break;

                    case 2:
                        heartRateChart.setVisibility(View.INVISIBLE);
                        combinedChart.setVisibility(View.INVISIBLE);
                        txtFeature.setVisibility(View.VISIBLE);
                        btnOrderToggle.setVisibility(View.GONE);
                        break;
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab t) {}
            @Override public void onTabReselected(TabLayout.Tab t) {}
        });

        btnOrderToggle.setOnClickListener((v) -> {
            isDescending = !isDescending;

            binding.btnOrderToggle.setIcon(
                    AppCompatResources.getDrawable(
                            requireContext(),
                            isDescending ? R.drawable.ic_arrow_down : R.drawable.ic_arrow_up
                    )
            );
            //Reload session in new order
            sessionViewModel.loadComparedSessions(userId, !isDescending);
        });
    }

    /**
     * Method to create the linechart:
     * 1) We read 1 session (the last one)
     * 2) We display a green line -> all points in a session
     * 3) We detect stress using
     * 3) We overlay yellow point
     * 4) We overlat red point
     */
    private void createHeartRateChartWithMinutes(List<String> session1) {
        Log.d("DashboardFragment", "session1=" + (session1 != null ? session1.size() + " items" : "null"));
        List<Float> bpm = new ArrayList<>();
        List<Float> hrv = new ArrayList<>();

        if (session1 != null) {
            //Compute BPM and HRV per minute (have same points)
            bpm = computeBPM(session1);
            hrv = computeHRV(session1);
            Log.d("DashboardFragment", "Computed BPM size=" + bpm.size() + ", HRV size=" + hrv.size());
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
        leftAxis.setAxisMaximum(max + 10);
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
        leftAxis.removeAllLimitLines();
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
     * Create box plot chart where box represents ±10 BPM around average
     * Outliers (>10 BPM from avg) shown as individual points
     * Whiskers always go from absolute min to max
     */
    private void createCandleChart(List<String> session1, List<String> session2) {

        List<Float> bpm1 = new ArrayList<>();
        List<Float> hrv1 = new ArrayList<>();
        List<Float> bpm2 = new ArrayList<>();
        List<Float> hrv2 = new ArrayList<>();

        //Compute BPM + HRV per session
        if (session1 != null && !session1.isEmpty()) {
            bpm1 = computeBPM(session1);
            hrv1 = computeHRV(session1);
        }

        if (session2 != null && !session2.isEmpty()) {
            bpm2 = computeBPM(session2);
            hrv2 = computeHRV(session2);
        }

        if (bpm1.isEmpty() && bpm2.isEmpty()) return;

        //Data structures
        List<CandleEntry> candleEntries = new ArrayList<>();
        List<Entry> medianEntries = new ArrayList<>();
        List<Entry> outlierEntries = new ArrayList<>();

        //Process Session 1
        if (!bpm1.isEmpty()) {
            BoxPlotData data = computeBoxPlotData(bpm1, 0);
            candleEntries.add(data.candleEntry);
            medianEntries.add(data.medianEntry);
            outlierEntries.addAll(data.outliers);
        }

        //Process Session 2
        if (!bpm2.isEmpty()) {
            BoxPlotData data = computeBoxPlotData(bpm2, 1);
            candleEntries.add(data.candleEntry);
            medianEntries.add(data.medianEntry);
            outlierEntries.addAll(data.outliers);
        }

        //Create CandleDataSet for box + whiskers
        CandleDataSet candleDataSet = new CandleDataSet(candleEntries, "BPM Distribution");
        candleDataSet.setShadowColor(Color.BLACK); //Whisker lines (min to max)
        candleDataSet.setShadowWidth(2f);
        candleDataSet.setDecreasingColor(Color.parseColor("#4A90E2")); //Box color
        candleDataSet.setDecreasingPaintStyle(Paint.Style.FILL);
        candleDataSet.setIncreasingColor(Color.parseColor("#4A90E2")); //Box color
        candleDataSet.setIncreasingPaintStyle(Paint.Style.FILL);
        candleDataSet.setNeutralColor(Color.parseColor("#4A90E2"));
        candleDataSet.setDrawValues(false);
        candleDataSet.setBarSpace(0.3f);

        CandleData candleData = new CandleData(candleDataSet);

        //Median markers (red squares)
        ScatterDataSet medianSet = new ScatterDataSet(medianEntries, "Average");
        medianSet.setColor(Color.RED);
        medianSet.setScatterShape(ScatterChart.ScatterShape.SQUARE);
        medianSet.setScatterShapeSize(12f);
        medianSet.setDrawValues(false);

        //Outlier points (black circles)
        ScatterDataSet outlierSet = new ScatterDataSet(outlierEntries, "Outliers");
        outlierSet.setColor(Color.BLACK);
        outlierSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        outlierSet.setScatterShapeSize(8f);
        outlierSet.setDrawValues(false);

        ScatterData scatterData = new ScatterData();
        scatterData.addDataSet(medianSet);
        if (!outlierEntries.isEmpty()) {
            scatterData.addDataSet(outlierSet);
        }

        //Combine all data
        CombinedData combinedData = new CombinedData();
        combinedData.setData(candleData);
        combinedData.setData(scatterData);

        //Configure chart for BPM data
        combinedChart.setData(combinedData);
        combinedChart.getDescription().setEnabled(false);

        //Right axis disabled
        combinedChart.getAxisRight().setEnabled(false);

        //Left Y-axis (BPM values)
        float maxBpm = computeMax(bpm1, bpm2) + 15;
        float axisMax = Math.max(100, maxBpm);
        YAxis leftAxis = combinedChart.getAxisLeft();
        leftAxis.setAxisMinimum(40f);
        leftAxis.setAxisMaximum(axisMax);
        leftAxis.setGranularity(10f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisLineWidth(1f);
        leftAxis.setTextSize(12f);

        //X-axis (Sessions)
        XAxis xAxis = combinedChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setAxisMinimum(-0.5f);
        xAxis.setAxisMaximum(1.5f);
        xAxis.setDrawGridLines(false);
        xAxis.setAxisLineWidth(1f);
        xAxis.setTextSize(12f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return "Session " + ((int)value + 1);
            }
        });

        //Legend
        Legend legend = combinedChart.getLegend();
        legend.setEnabled(true);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);

        combinedChart.setDrawGridBackground(false);
        combinedChart.setPinchZoom(false);
        combinedChart.setScaleEnabled(false);
        combinedChart.setExtraBottomOffset(10f);
        combinedChart.setExtraTopOffset(10f);

        //Marker
        combinedChart.setMarker(
                new CandleMarkerView(
                        getContext(),
                        bpm1, hrv1,
                        bpm2, hrv2,
                        (int) userBaseline_hr,
                        userBaseline_hrv
                )
        );

        combinedChart.invalidate();
    }

    /**
     * Helper class to hold box plot data
     */
    private static class BoxPlotData {
        CandleEntry candleEntry;
        Entry medianEntry;
        List<Entry> outliers;

        BoxPlotData(CandleEntry candle, Entry median, List<Entry> out) {
            this.candleEntry = candle;
            this.medianEntry = median;
            this.outliers = out;
        }
    }

    /**
     * Compute box plot data with outlier detection
     * Box = avg ± 10 BPM (clamped to min/max)
     * Outliers = values > 10 BPM away from avg
     * Whiskers = always min to max
     */
    private BoxPlotData computeBoxPlotData(List<Float> data, int xPosition) {
        if (data.isEmpty()) {
            return new BoxPlotData(
                    new CandleEntry(xPosition, 0, 0, 0, 0),
                    new Entry(xPosition, 0),
                    new ArrayList<>()
            );
        }

        //Calculate statistics
        float sum = 0;
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;

        for (float value : data) {
            sum += value;
            if (value < min) min = value;
            if (value > max) max = value;
        }

        float avg = sum / data.size();

        //Box range: avg ± 10, but clamped to actual data range
        float boxLow = Math.max(avg - 10f, min);
        float boxHigh = Math.min(avg + 10f, max);

        //Find outliers (>10 BPM from average)
        List<Entry> outliers = new ArrayList<>();
        for (float value : data) {
            if (Math.abs(value - avg) > 10f) {
                outliers.add(new Entry(xPosition, value));
            }
        }

        //CandleEntry(x, shadowHigh, shadowLow, open, close)
        //shadowHigh/Low = whiskers (always min to max)
        //open/close = box (avg ± 10, clamped)
        CandleEntry candleEntry = new CandleEntry(
                xPosition,
                max,        //shadowHigh (whisker top)
                min,        //shadowLow (whisker bottom)
                boxLow,     //open (box bottom)
                boxHigh     //close (box top)
        );

        Entry medianEntry = new Entry(xPosition, avg);

        return new BoxPlotData(candleEntry, medianEntry, outliers);
    }

//    private void loadUserSessionsAndComputeChart() {
//        executor.execute(() -> {
//            List<SessionData> userSessions = appDatabase.sessionDataDao().getSessionsForUser(userId);
//
//            //Convert SessionData -> Map<Integer, List<String>>
//            Map<Integer, List<String>> sessionsMap = new HashMap<>();
//            int index = 1;
//            for (SessionData s : userSessions) {
//                sessionsMap.put(index++, s.getSessionTS());
//            }
//
//            //Update fragment's field and charts on the main thread
//            requireActivity().runOnUiThread(() -> {
//                this.sessions = sessionsMap; //assign to fragment field
//                createHeartRateChartWithMinutes();
//                createCandleChart();
//            });
//        });
//    }

    private int computeMax(List<Float> session1, List<Float> session2) {
        float max = 0f;

        for (float value : session1) {
            if (value > max) {
                max = value;
            }
        }

        for (float value : session2) {
            if (value > max) {
                max = value;
            }
        }

        return Math.round(max);
    }

    private void observeBaselines() {
        DashboardViewModel dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        Log.d("DashboardFragment", "Loading baselines for userId: " + userId);

        dashboardViewModel.getBaselineHr(userId);
        dashboardViewModel.getBaselineHrv(userId);

        dashboardViewModel.getUserBaselineHr().observe(getViewLifecycleOwner(), baselineHr -> {
            Log.d("DashboardFragment", "Baseline HR observer triggered: " + baselineHr);
            if (baselineHr != null && baselineHr != 0) {
                this.userBaseline_hr = baselineHr;
                //updateChartsIfReady();
            }
        });

        dashboardViewModel.getUserBaselineHrv().observe(getViewLifecycleOwner(), baselineHrv -> {
            Log.d("DashboardFragment", "Baseline HRV observer triggered: " + baselineHrv);
            if (baselineHrv != null && baselineHrv != 0) {
                this.userBaseline_hrv = baselineHrv;
                //updateChartsIfReady();
            }
        });
    }

//    private void updateChartsIfReady() {
//        // Check if we have all necessary data
//        if (userBaseline_hr == 0 || userBaseline_hrv == 0) {
//            Log.d("DashboardFragment", "Baselines not ready yet");
//            return;
//        }
//
//        // Update line chart if data is available
//        if (lineSession != null && !lineSession.isEmpty()) {
//            Log.d("DashboardFragment", "Creating line chart");
//            createHeartRateChartWithMinutes();
//        }
//
//        // Update boxplot if data is available
//        if (boxplotSessions != null && !boxplotSessions.isEmpty()) {
//            Log.d("DashboardFragment", "Creating boxplot");
//            createCandleChart();
//        }
//    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}