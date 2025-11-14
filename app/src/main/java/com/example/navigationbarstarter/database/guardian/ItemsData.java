package com.example.navigationbarstarter.database.guardian;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "item")
public class ItemsData {

    @PrimaryKey(autoGenerate = true)
    private long id;

    //Magic hat ..
    @NonNull
    @ColumnInfo(name = "name")
    private String name;

    @NonNull
    @ColumnInfo(name = "rarity")
    private Rarity rarity;

//    @ColumnInfo(name = "unlocked")
//    private boolean unlocked;

    @ColumnInfo(name = "price_to_unlock")
    private long priceToUnlock;

    @NonNull
    @ColumnInfo(name = "type")
    private Type type;

    @ColumnInfo(name = "image_res_id")
    private int imageResId;

    public ItemsData (@NonNull String name, @NonNull Rarity rarity, long priceToUnlock, @NonNull Type type, int imageResId) {
        this.name = name;
        this.rarity = rarity;
        this.priceToUnlock = priceToUnlock;
        this.type = type;
        this.imageResId = imageResId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    @NonNull
    public Rarity getRarity() {
        return rarity;
    }

    public void setRarity(@NonNull Rarity rarity) {
        this.rarity = rarity;
    }

    public long getPriceToUnlock() {
        return priceToUnlock;
    }

    public void setPriceToUnlock(long priceToUnlock) {
        this.priceToUnlock = priceToUnlock;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public int getImageResId() {
        return imageResId;
    }

    public void setImageResId(int imageResId) {
        this.imageResId = imageResId;
    }
}
