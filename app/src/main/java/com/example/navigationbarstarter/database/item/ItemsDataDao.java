package com.example.navigationbarstarter.database.item;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface ItemsDataDao {

    //Insert Items in the db, replacing if a data is already there (no problem since are fixed items)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertItem(ItemsData item);

    //Same but inserting a list
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertItems(List<ItemsData> items);

    //Returns a list of items by type
    @Query("SELECT * FROM item WHERE type = :type")
    List<ItemsData> getItemsByType(Type type);

    //Returning the counter of all items in the db (mark as transaction to avoid race condition)
    @Transaction
    @Query("SELECT COUNT(*) FROM item")
    int getItemsCount();

    //Returns an item, given the id
    @Query("SELECT * FROM item WHERE id = :id LIMIT 1")
    ItemsData getItemById(long id);

    @Query("SELECT * FROM item")
    List<ItemsData> getAllItems();

    @Query("SELECT id FROM item WHERE name = :itemName")
    long getBaseItemId(String itemName);
}
