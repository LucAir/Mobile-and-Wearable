package com.example.navigationbarstarter.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.navigationbarstarter.database.guardian.GuardianData;
import com.example.navigationbarstarter.database.guardian.GuardianDataDao;
import com.example.navigationbarstarter.database.item.ItemsData;
import com.example.navigationbarstarter.database.item.ItemsDataDao;

@Database(entities = {UserData.class,
                      ModeChange.class,
                      GuardianData.class,
                      ItemsData.class
                      }, version = 9, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;
    private static final Object LOCK  = new Object();

    public abstract UserDataDao userDataDao();
    public abstract ModeChangeDao modeChangeDao();
    public abstract GuardianDataDao guardianDataDao();
    public abstract ItemsDataDao itemsDataDao();

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