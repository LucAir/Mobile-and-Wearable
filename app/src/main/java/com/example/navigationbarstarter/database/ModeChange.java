package com.example.navigationbarstarter.database;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "mode_changes")
public class ModeChange {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @NonNull
    @ColumnInfo(name = "mode_type")
    private String modeType; // "Focus Mode", "Break Mode", "Rest Mode"

    @NonNull
    @ColumnInfo(name = "start_timestamp")
    private long startTimestamp;

    @ColumnInfo(name = "end_timestamp")
    private long endTimestamp;

    @ColumnInfo(name = "heart_rate")
    private int heartRate;

    @ColumnInfo(name = "duration_seconds")
    private long durationSeconds;

    public ModeChange(@NonNull String modeType, long startTimestamp, int heartRate) {
        this.modeType = modeType;
        this.startTimestamp = startTimestamp;
        this.heartRate = heartRate;
        this.endTimestamp = 0;
        this.durationSeconds = 0;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @NonNull
    public String getModeType() {
        return modeType;
    }

    public void setModeType(@NonNull String modeType) {
        this.modeType = modeType;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public long getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(long endTimestamp) {
        this.endTimestamp = endTimestamp;
        if (startTimestamp > 0) {
            this.durationSeconds = (endTimestamp - startTimestamp) / 1000;
        }
    }

    public int getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(int heartRate) {
        this.heartRate = heartRate;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }
}
