package com.example.navigationbarstarter.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {UserData.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    //Get instance (volatile -> ensures each thread reads the same variable)
    private static volatile AppDatabase instance;

    //LOCK -> avoid overlap -> only one thread at time can use something
    private static final Object LOCK  = new Object();

    public abstract UserDataDao userDataDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "app_database"
                    )
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return instance;
    }

    public static void destroyInstance() {
        synchronized (LOCK) {
            if (instance != null && instance.isOpen()) {
                instance.close();
            }
            instance = null;
        }
    }


}
