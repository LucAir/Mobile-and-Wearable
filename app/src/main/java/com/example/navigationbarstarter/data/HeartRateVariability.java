package com.example.navigationbarstarter.data;

import java.util.ArrayList;
import java.util.List;

public class HeartRateVariability {

    /*
        This class aims to compute HRV. Is based on RR intervals:
            - IBI -> inter-beat intervals
            - NN intervals -> normal-to-normal beats
        RR interval is the time between 2 heartbeats measured in milliseconds
        So given:
        Beat            Time
        1               0
        2               850
        3               1700

        => RR intervals [850 ms, 850 ms]

        HRV describes how much those intervals vary

        There are 3 way to compute HRV:
            1)RMSSD -> Root Square of Successive Differences -> used for short-term
            2)SDNN -> standard deviation of RR intervals -> good for long-term HRV
            3)pNN50 -> percentage of consecutive RR intervals differing by > 50 ms

        In our data we do not have the right timestamp -> it indicates when the device check the hearbeat, but its not constant every millisecond
        So I decided to round things up to see if works, and then eventually considering taking a real dataset.
        BPM = (beats per minute)1 beat = t = 60,000 ms -> So RR (time per beat) = 60,000 / BPM.
        After doing this approximation we have a series of RR so whe can compute HRV
     */

    public static double bpmToRR(int bpm) {
        return 60000.0 / bpm;
    }

    //Compute RMSSD
    public static double computeRMSSD(List<Integer> bpmValues) {
        if (bpmValues.size() < 2) return 0;

        List<Double> rrList = new ArrayList<>();
        for (int bpm : bpmValues) {
            rrList.add(bpmToRR(bpm));
        }

        List<Double> diffs = new ArrayList<>();

        List<Double> squares = new ArrayList<>();
        for (int i = 1; i < rrList.size(); i++) {
            double diff = rrList.get(i) - rrList.get(i - 1);
            squares.add(diff * diff); //squared difference
        }

        double mean = 0;
        for (double sq : squares) mean += sq;
        mean /= squares.size();

        return Math.sqrt(mean); //final RMSSD value
    }

    //Compute SDNN
    public static double computeSDNN(List<Integer> bpmValues) {
        if (bpmValues.isEmpty()) return 0;

        List<Double> rrList = new ArrayList<>();
        for (int bpm : bpmValues) rrList.add(bpmToRR(bpm));

        double mean = 0;
        for (double rr : rrList) mean += rr;
        mean /= rrList.size();

        double variance = 0;
        for (double rr : rrList) variance += Math.pow(rr - mean, 2);
        variance /= rrList.size();

        return Math.sqrt(variance);
    }
}
