package com.example.navigationbarstarter.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.LinkedList;
import java.util.List;

import javax.xml.namespace.QName;

@Entity(tableName = "userdata",
        //Creating index for fast access (retrieve user by email/username)
        //Useful for concurrency also, because are unique, so even if a thread write Luca as username,
        //and there is already, it will be rejected.
        indices = {
            @Index(value = "username", unique = true),
            @Index(value = "email", unique = true)
        })

public class UserData {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "surname")
    private String surname;

    @NonNull
    @ColumnInfo(name = "age")
    private int age;

    @NonNull
    @ColumnInfo(name = "email")
    private String email;

    @NonNull
    @ColumnInfo(name = "username")
    private String username;

    @NonNull
    @ColumnInfo(name = "password")
    private String password;

    @NonNull
    @ColumnInfo(name = "guardianId")
    private long guardianId;

    @NonNull
    @ColumnInfo(name = "list_unlocked_items")
    List<Long> unlockedItems;

    @ColumnInfo(name = "token")
    long token;

    @ColumnInfo(name = "profile_image_uri")
    private String profileImageUri;

    @ColumnInfo(name = "first_log_in")
    private boolean isFirstLogin;

    @ColumnInfo(name = "baseline_hr")
    private float baselineHr;

    @ColumnInfo(name = "baseline_hrv")
    private float baselineHrv;

    public UserData(@NonNull String username,
                    @NonNull int age,
                    @NonNull String email,
                    @NonNull String password,
                    @NonNull long guardianId,
                    @NonNull List<Long> unlockedItems,
                    long token,
                    @Nullable String name,
                    @Nullable String surname,
                    @Nullable String profileImageUri,
                    @Nullable boolean isFirstLogin,
                    @Nullable float baselineHr,
                    @Nullable float baselineHrv
                    ) {
        this.age = age;
        this.email = email;
        this.username = username;
        this.password = password;
        this.guardianId = guardianId;
        this.unlockedItems = unlockedItems;
        this.token = token;
        this.name = name;
        this.surname = surname;
        this.profileImageUri = profileImageUri;
        this.isFirstLogin = isFirstLogin;
        this.baselineHr = baselineHr;
        this.baselineHrv = baselineHrv;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    @NonNull
    public String getUsername() {
        return username;
    }

    public void setUsername(@NonNull String username) {
        this.username = username;
    }

    @NonNull
    public String getEmail() {
        return email;
    }

    public long getGuardianId() {
        return guardianId;
    }

    public void setGuardianId(long guardianId) {
        this.guardianId = guardianId;
    }

    public void setEmail(@NonNull String email) {
        this.email = email;
    }

    @NonNull
    public String getPassword() {
        return password;
    }

    public void setPassword(@NonNull String password) {
        this.password = password;
    }

    @NonNull
    public List<Long> getUnlockedItems() {
        return unlockedItems;
    }

    public void setUnlockedItems(@NonNull List<Long> unlockedItems) {
        this.unlockedItems = unlockedItems;
    }

    public long getToken() {
        return token;
    }

    public void setToken(long token) {
        this.token = token;
    }

    public String getProfileImageUri() {
        return profileImageUri;
    }

    public void setProfileImageUri(String profileImageUri) {
        this.profileImageUri = profileImageUri;
    }

    public boolean isFirstLogin() {
        return isFirstLogin;
    }

    public void setFirstLogin(boolean firstLogin) {
        isFirstLogin = firstLogin;
    }

    public float getBaselineHr() {
        return baselineHr;
    }

    public void setBaselineHr(float baselineHr) {
        this.baselineHr = baselineHr;
    }

    public float getBaselineHrv() {
        return baselineHrv;
    }

    public void setBaselineHrv(float baselineHrv) {
        this.baselineHrv = baselineHrv;
    }
}

