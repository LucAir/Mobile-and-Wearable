package com.example.navigationbarstarter;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.example.navigationbarstarter.database.AppDatabase;
import com.example.navigationbarstarter.database.item.InitializeItems;
import com.example.navigationbarstarter.database.item.ItemsData;
import com.example.navigationbarstarter.ui.guardian.GuardianRepository;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.navigationbarstarter.databinding.ActivityMainBinding;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private static final String PREFS = "app_prefs";
    AppDatabase appDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        appDatabase = AppDatabase.getInstance(getApplicationContext());
        Executor executor = Executors.newSingleThreadExecutor();
        GuardianRepository repo = new GuardianRepository(getApplicationContext());

        //Getting user preferences to understand if some items have been added to list and pull eventually
        SharedPreferences preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        int itemsVersion = preferences.getInt("items_version", 0);

        //TEST
        Log.d("MainTest", "MainActivity started");

        executor.execute(() -> {
            int count = appDatabase.itemsDataDao().getItemsCount();
            Log.d("MainActivityTest","Number of element: " + count);
            if (count == 0 || itemsVersion < InitializeItems.ITEM_VERSION) {
                Log.d("MainActivityTest", "Initializing items...");
                appDatabase.itemsDataDao().insertItems(InitializeItems.initializeCollectiblesForUser());

                //Save new version
                preferences.edit().putInt("items_version", InitializeItems.ITEM_VERSION).apply();
            } else {
                Log.d("MainActivityTest", "Items already up-to-date. Skipping insert.");
            }
        });

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_guardian, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.BODY_SENSORS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.BODY_SENSORS}, 1);
            }
        }
    }

}