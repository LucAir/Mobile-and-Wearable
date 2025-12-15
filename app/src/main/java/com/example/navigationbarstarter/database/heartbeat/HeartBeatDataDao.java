package com.example.navigationbarstarter.database.heartbeat;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface HeartBeatDataDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(HeartBeatData heartBeatData);

    @Delete
    int delete(HeartBeatData heartBeatData);

    @Query("SELECT * FROM heartbeat WHERE userId = :id")
    HeartBeatData getDataByUserId(long id);
}

