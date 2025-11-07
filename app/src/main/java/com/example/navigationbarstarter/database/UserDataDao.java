package com.example.navigationbarstarter.database;

import static android.icu.text.MessagePattern.ArgType.SELECT;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface UserDataDao {
    @Insert
    void insert(UserData userData);

    @Delete
    int delete(UserData userData);

    @Query("SELECT password FROM USERDATA WHERE id = id" )
    String getPassord(int id);

}
