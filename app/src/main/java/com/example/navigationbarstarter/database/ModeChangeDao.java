

// ModeChangeDao.java
package com.example.navigationbarstarter.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ModeChangeDao {

    @Insert
    long insert(ModeChange modeChange);

    @Update
    void update(ModeChange modeChange);

    @Query("SELECT * FROM mode_changes ORDER BY start_timestamp DESC")
    List<ModeChange> getAllModeChanges();

    @Query("SELECT * FROM mode_changes WHERE start_timestamp >= :startTime AND start_timestamp <= :endTime ORDER BY start_timestamp DESC")
    List<ModeChange> getModeChangesBetween(long startTime, long endTime);

    @Query("SELECT * FROM mode_changes WHERE mode_type = :modeType ORDER BY start_timestamp DESC")
    List<ModeChange> getModeChangesByType(String modeType);

    @Query("SELECT * FROM mode_changes ORDER BY start_timestamp DESC LIMIT 1")
    ModeChange getLatestModeChange();

    @Query("SELECT SUM(duration_seconds) FROM mode_changes WHERE mode_type = :modeType AND start_timestamp >= :startTime")
    long getTotalDurationForMode(String modeType, long startTime);

    @Query("DELETE FROM mode_changes")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM mode_changes WHERE mode_type = :modeType AND start_timestamp >= :startTime")
    int getModeSwitchCount(String modeType, long startTime);

}