package com.example.navigationbarstarter.database.guardian;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "guardian")
public class GuardianData {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "guardianId")
    private long guardianId;

    @NonNull
    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "equipped_skin")
    private long equippedSkin;

    @ColumnInfo(name = "equipped_pet")
    private long equippedPet;

    @NonNull
    @ColumnInfo(name = "equipped_background")
    private long equippedBackground;

    public GuardianData() {
        //Initialize with default values, then user can update it
        this.name = "Guardian";
        this.equippedBackground = 3;
        this.equippedPet = 4;
    }

    public long getGuardianId() {
        return guardianId;
    }

    public void setGuardianId(long guardianId) {
        this.guardianId = guardianId;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public long getEquippedSkin() {
        return equippedSkin;
    }

    public void setEquippedSkin(long equippedSkin) {
        this.equippedSkin = equippedSkin;
    }

    public long getEquippedPet() {
        return equippedPet;
    }

    public void setEquippedPet(long equippedPet) {
        this.equippedPet = equippedPet;
    }

    public long getEquippedBackground() {
        return equippedBackground;
    }

    public void setEquippedBackground(long equippedBackground) {
        this.equippedBackground = equippedBackground;
    }

    public List<Long> getEquippedItems() {
        List<Long> equippedItems = new ArrayList<>();
        equippedItems.add(this.equippedSkin);
        equippedItems.add(this.equippedBackground);
        equippedItems.add(this.equippedPet);
        return equippedItems;
    }
}
