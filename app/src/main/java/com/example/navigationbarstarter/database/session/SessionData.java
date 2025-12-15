package com.example.navigationbarstarter.database.session;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import com.example.navigationbarstarter.database.UserData;

import java.util.List;

@Entity(tableName = "sessions",
        foreignKeys = @ForeignKey(
                entity = UserData.class,
                parentColumns = "id",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE)
        )

public class SessionData {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "sessionTS")
    private List<String> sessionTS;

    //FK to user
    @ColumnInfo(name = "userId")
    private long userId;

    @ColumnInfo(name = "createdAt")
    private long createdAt;

    //Constructors
    public SessionData() {
    }

    //Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public List<String> getSessionTS() {
        return sessionTS;
    }

    public void setSessionTS(List<String> sessionTS) {
        this.sessionTS = sessionTS;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
