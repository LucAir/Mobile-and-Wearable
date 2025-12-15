package com.example.navigationbarstarter.database.guardian;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface GuardianDataDao {

    //Creating a new guardian in the db
    @Insert
    long insert(GuardianData guardian);

    //Update a guardian (changing skin)
    @Update
    void updateGuardian(GuardianData guardianData);

    //Returning a guardian given the id
    @Query("SELECT * FROM guardian WHERE guardianId = :id LIMIT 1")
    GuardianData getGuardianById(long id);
}
