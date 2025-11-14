package com.example.navigationbarstarter.database;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.example.navigationbarstarter.database.guardian.ItemsData;

import java.util.List;

@Entity(tableName = "userdata",
        //Creating index for fast access (retrieve user by email/username)
        //Useful for concurrency also, because are unique, so even if a thread write Luca as username,
        //and there is already, it will be rejected.
        indices = {
            @Index(value = "username", unique = true),
            @Index(value = "email", unique = true)
        })

//TODO: maybe add AGE for statistics?? Heart rate (more higher/lower -> adapt that to give better notification?)
public class UserData {

    @PrimaryKey(autoGenerate = true)
    private int id;

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
    List<ItemsData> unlockedItems;


    public UserData(@NonNull String username, @NonNull int age, @NonNull String email, @NonNull String password, @NonNull long guardianId, @NonNull List<ItemsData> unlockedItems) {
        this.name = name;
        this.surname = surname;
        this.age = age;
        this.email = email;
        this.username = username;
        this.password = password;
        this.guardianId = guardianId;
        this.unlockedItems = unlockedItems;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
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
}

