package com.example.navigationbarstarter.database.session;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SessionDataDao {
    @Insert
    long insertSession(SessionData session);

    @Query("SELECT * FROM sessions WHERE userId = :userId ORDER BY createdAt ASC")
    List<SessionData> getSessionsForUser(long userId);

    @Query("""
        SELECT * FROM sessions
        WHERE userId = :userId
        ORDER BY createdAt ASC
    """)
    List<SessionData> getSessionsByUserAsc(long userId);

    @Query("""
        SELECT * FROM sessions
        WHERE userId = :userId
        ORDER BY createdAt DESC
    """)
    List<SessionData> getSessionsByUserDesc(long userId);
}
