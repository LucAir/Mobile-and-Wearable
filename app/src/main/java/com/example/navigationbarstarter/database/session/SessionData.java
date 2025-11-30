package com.example.navigationbarstarter.database.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import androidx.room.Index;

import com.example.navigationbarstarter.database.UserData;

@Entity(tableName = "sessions",
        foreignKeys = @ForeignKey(
                entity = UserData.class,
                parentColumns = "id",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("userId")})
public class SessionData {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long userId;

    //Timestamp in milliseconds
    @NonNull
    private long startTime;

    //Nullable - null means session is still active
    @Nullable
    private Long endTime;

    //Calculated when session ends
    private Integer avgHeartRate;

    //Max BPM during session
    private Integer maxHeartRate;

    //Min BPM during session
    private Integer minHeartRate;

    //Optional notes about the session
    private String notes;

    // Constructors
    public SessionData() {
    }

    public SessionData(long userId, long startTime) {
        this.userId = userId;
        this.startTime = startTime;
    }

    //Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public Integer getAvgHeartRate() {
        return avgHeartRate;
    }

    public void setAvgHeartRate(Integer avgHeartRate) {
        this.avgHeartRate = avgHeartRate;
    }

    public Integer getMaxHeartRate() {
        return maxHeartRate;
    }

    public void setMaxHeartRate(Integer maxHeartRate) {
        this.maxHeartRate = maxHeartRate;
    }

    public Integer getMinHeartRate() {
        return minHeartRate;
    }

    public void setMinHeartRate(Integer minHeartRate) {
        this.minHeartRate = minHeartRate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    //Helper methods
    public long getDuration() {
        if (endTime == null) {
            return System.currentTimeMillis() - startTime;
        }
        return endTime - startTime;
    }

    public boolean isActive() {
        return endTime == null;
    }
}
