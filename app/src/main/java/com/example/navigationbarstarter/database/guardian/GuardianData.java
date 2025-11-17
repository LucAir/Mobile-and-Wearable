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

    @ColumnInfo(name = "equipped_hat")
    private long equippedHat;

    @ColumnInfo(name = "equipped_tshirt")
    private long equippedTshirt;


    @ColumnInfo(name = "equipped_pet")
    private long equippedPet;

    @ColumnInfo(name = "equipped_aura")
    private long equippedAura;

    @NonNull
    @ColumnInfo(name = "equipped_background")
    private long equippedBackground;

    public GuardianData() {
        //Initialize with default values, then user can update it
        this.name = "Guardian";
        this.equippedHat = 0;
        this.equippedTshirt = 1;
        this.equippedAura = 2;
        this.equippedBackground = 3;
        this.equippedPet = 4;
    }

//    public GuardianData(String name, long equippedHat, long equippedTshirt, long equippedAura, long equippedBackground, long equippedPet) {
//        this.name = name;
//        this.equippedHat = equippedHat;
//        this.equippedTshirt = equippedTshirt;
//        this.equippedAura = equippedAura;
//        this.equippedBackground = equippedBackground;
//        this.equippedPet = equippedPet;
//    }

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

    public long getEquippedHat() {
        return equippedHat;
    }

    public void setEquippedHat(long equippedHat) {
        this.equippedHat = equippedHat;
    }

    public long getEquippedTshirt() {
        return equippedTshirt;
    }

    public void setEquippedTshirt(long equippedTshirt) {
        this.equippedTshirt = equippedTshirt;
    }

    public long getEquippedPet() {
        return equippedPet;
    }

    public void setEquippedPet(long equippedPet) {
        this.equippedPet = equippedPet;
    }

    public long getEquippedAura() {
        return equippedAura;
    }

    public void setEquippedAura(long equippedAura) {
        this.equippedAura = equippedAura;
    }

    public long getEquippedBackground() {
        return equippedBackground;
    }

    public void setEquippedBackground(long equippedBackground) {
        this.equippedBackground = equippedBackground;
    }

    public List<Long> getEquippedItems() {
        List<Long> equippedItems = new ArrayList<>();
        equippedItems.add(this.equippedHat);
        equippedItems.add(this.equippedTshirt);
        equippedItems.add(this.equippedAura);
        equippedItems.add(this.equippedBackground);
        equippedItems.add(this.equippedPet);
        return equippedItems;
    }
}
