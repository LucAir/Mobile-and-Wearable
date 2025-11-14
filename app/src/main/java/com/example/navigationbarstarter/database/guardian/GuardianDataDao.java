package com.example.navigationbarstarter.database.guardian;

import androidx.room.Dao;
import androidx.room.Insert;

@Dao
public interface GuardianDataDao {

    @Insert
    long insert(GuardianData guardian);


}
