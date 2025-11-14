package com.example.navigationbarstarter.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;

@Database(entities = {UserData.class, ModeChange.class}, version = 4, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;
    private static final Object LOCK  = new Object();

    public abstract UserDataDao userDataDao();
    public abstract ModeChangeDao modeChangeDao();

    @TypeConverters({Converters.class})
    public abstract class AppDatabase extends RoomDatabase {
        public static synchronized AppDatabase getInstance(Context context)
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