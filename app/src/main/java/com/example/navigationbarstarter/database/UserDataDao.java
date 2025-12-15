package com.example.navigationbarstarter.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

@Dao
public interface UserDataDao {
    //Using ABORT as a conflictStrategy, to return an error when we try to insert 2 times same user
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(UserData userData);

    @Delete
    int delete(UserData userData);

    @Transaction
    @Query("SELECT * FROM userdata WHERE username = :username AND password = :password LIMIT 1")
    UserData login(String username, String password);

    @Query("SELECT * FROM userdata WHERE email = :email LIMIT 1")
    UserData getUserByEmail(String email);

    @Query("SELECT * FROM userdata WHERE id = :id LIMIT 1")
    UserData getUserById(long id);

    @Query("SELECT COUNT(*) FROM userdata")
    int getUserCount();

    @Query("DELETE FROM userdata")
    void deleteAll();

    @Query("SELECT * FROM userdata WHERE username = :username")
    UserData getUserByUsername(String username);

    //Update a user
    @Update
    void updateUser(UserData userData);

    @Query("SELECT guardianId FROM userdata WHERE id = :userId")
    long getGuardianId(long userId);

    @Query("SELECT token FROM userdata WHERE id = :userId")
    long getTokenNumber(long userId);

    @Query("SELECT * FROM userdata WHERE username = :username AND id != :currentUserId LIMIT 1")
    UserData getUserByUsernameExcludingCurrent(String username, long currentUserId);

    @Query("SELECT * FROM userdata WHERE username = :email AND id != :currentUserId LIMIT 1")
    UserData getUserByEmailExcludingCurrent(String email, long currentUserId);
}
