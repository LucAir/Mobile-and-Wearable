package com.example.navigationbarstarter.database.heartbeat;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.example.navigationbarstarter.database.UserData;

@Entity(
        tableName = "heartbeat",
        foreignKeys = @ForeignKey(
                entity = UserData.class,                 // Parent table
                parentColumns = "userId",            // Column in parent
                childColumns = "userId",             // Column in this table
                onDelete = ForeignKey.CASCADE        // Optional: delete heartbeats when user is deleted
        ),
        indices = @Index(value = "userId")       // Recommended for foreign keys
)

public class HeartBeatData {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    @ColumnInfo(name = "timestamp")
    private long timestamp;

    @NonNull
    @ColumnInfo(name = "bpm")
    private long bpm;

    @ColumnInfo(index = true)
    private long userId;



}
