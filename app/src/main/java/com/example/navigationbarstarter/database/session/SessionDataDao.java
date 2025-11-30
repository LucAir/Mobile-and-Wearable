package com.example.navigationbarstarter.database.session;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface SessionDataDao {
    @Insert
    long insertSession(SessionData session);

    @Update
    void updateSession(SessionData session);

    @Delete
    void deleteSession(SessionData session);

    @Query("SELECT * FROM sessions WHERE userId = :userId ORDER BY startTime DESC")
    List<SessionData> getAllSessionsForUser(long userId);

    @Query("SELECT * FROM sessions WHERE userId = :userId ORDER BY startTime DESC LIMIT :limit")
    List<SessionData> getLastNSessions(long userId, int limit);

    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    SessionData getSessionById(long sessionId);

    @Query("SELECT * FROM sessions WHERE userId = :userId AND endTime IS NULL LIMIT 1")
    SessionData getActiveSession(long userId);

    @Query("SELECT * FROM sessions WHERE userId = :userId AND startTime >= :startTime AND endTime <= :endTime ORDER BY startTime DESC")
    List<SessionData> getSessionsInTimeRange(long userId, long startTime, long endTime);

    @Query("SELECT COUNT(*) FROM sessions WHERE userId = :userId")
    int getSessionCount(long userId);

    @Query("SELECT AVG(avgHeartRate) FROM sessions WHERE userId = :userId AND avgHeartRate IS NOT NULL")
    Double getAverageHeartRateAcrossSessions(long userId);

    @Query("SELECT MAX(maxHeartRate) FROM sessions WHERE userId = :userId")
    Integer getMaxHeartRateAllTime(long userId);
}
