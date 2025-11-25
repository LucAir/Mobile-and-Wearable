package com.example.navigationbarstarter.ui.guardian;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.navigationbarstarter.database.AppDatabase;
import com.example.navigationbarstarter.database.UserData;
import com.example.navigationbarstarter.database.guardian.GuardianData;
import com.example.navigationbarstarter.database.item.ItemsData;
import com.example.navigationbarstarter.database.item.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GuardianViewModel extends AndroidViewModel {

    private final GuardianRepository guardianRepository;
    private AppDatabase database;
    private Executor executor;

    // We only need ONE LiveData for the list of items
    private final MutableLiveData<List<ItemsData>> itemsLive = new MutableLiveData<>();

    private final MutableLiveData<GuardianData> guardianLive = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>();
    private final MutableLiveData<String> message = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<List<Long>> equippedItemsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Long>> userUnlockedItemsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Map<Long, ItemsData>> itemsMapLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<ItemsData>> allItemsLiveData = new MutableLiveData<>();

    public GuardianViewModel(@NonNull Application application) {
        super(application);
        database = AppDatabase.getInstance(application.getApplicationContext());
        guardianRepository = new GuardianRepository(application.getApplicationContext());
        executor = Executors.newSingleThreadExecutor();

        loadAllItems();
    }

    public void loadItemsByType(Type type) {
        loading.postValue(true);
        guardianRepository.loadItemsByType(type, items -> {
            itemsLive.postValue(items);
            loading.postValue(false);
        });
    }

    public void equipItem(long guardianId, ItemsData item, Runnable onComplete) {
        loading.postValue(true);
        guardianRepository.equipItem(guardianId, item, () -> {
            // DB is updated, force reload of the live data
            loadEquippedItemsForGuardian(guardianId);
            loading.postValue(false);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void loadGuardian(long guardianId){
        loading.postValue(true);
        executor.execute(() -> {
            GuardianData guardianData = database.guardianDataDao().getGuardianById(guardianId);
            guardianLive.postValue(guardianData);
            loading.postValue(false);
        });
    }

    public void loadItems(Type selectedType) {
        loading.postValue(true);
        executor.execute(() -> {
            List<ItemsData> items = database.itemsDataDao().getItemsByType(selectedType);
            itemsLive.postValue(items);
            loading.postValue(false);
        });
    }

    public MutableLiveData<GuardianData> getGuardianLiveData() {
        return guardianLive;
    }

    public MutableLiveData<List<Long>> getEquippedItemsLiveData() {
        return equippedItemsLiveData;
    }

    public MutableLiveData<List<ItemsData>> getItemsListLiveData(){
        return itemsLive;
    }

    public void loadEquippedItemsForGuardian(long guardianId) {
        loading.postValue(true);
        guardianRepository.loadEquippedItems(guardianId, items -> {
            equippedItemsLiveData.postValue(items);
            loading.postValue(false);
        });
    }

    public LiveData<List<Long>> getUserUnlockedItemsLiveData() {
        return userUnlockedItemsLiveData;
    }

    public void loadUserUnlockedItems(long userId) {
        executor.execute(() -> {
            UserData userData = database.userDataDao().getUserById(userId);
            List<Long> unlockedItems = userData.getUnlockedItems();
            userUnlockedItemsLiveData.postValue(unlockedItems);
        });
    }

    public MutableLiveData<Boolean> getLoadingLiveData() { return loading; }
    public MutableLiveData<String> getErrorLiveData() { return error; }
    public MutableLiveData<String> getSuccessMessageLiveData() { return message; }

    public void unlockAndEquipItem(ItemsData item, long userToken, long priceTokenItem, long guardianId, long userId) {
        loading.postValue(true);
        // Pass a Runnable callback to know when DB finishes
        guardianRepository.unlockAndEquipItem(item, userToken, priceTokenItem, guardianId, userId, () -> {
            // 1. Reload equipped items so the preview updates instantly
            loadEquippedItemsForGuardian(guardianId);
            // 2. Reload unlocked items so the lock icon disappears instantly
            loadUserUnlockedItems(userId);
            loading.postValue(false);
        });
    }

    // --- Helper Methods ---

    // This needs to run at start to build the ID -> Image map
    private void loadAllItems(){
        executor.execute(() -> {
            List<ItemsData> allItems = guardianRepository.getAllItems();
            allItemsLiveData.postValue(allItems);
            buildItemsMap(allItems);
        });
    }

    private void buildItemsMap(List<ItemsData> allItems) {
        if (allItems == null) return;
        Map<Long, ItemsData> map = new HashMap<>();
        for (ItemsData itemsData : allItems) {
            map.put(itemsData.getId(), itemsData);
        }
        itemsMapLiveData.postValue(map);
    }

    public MutableLiveData<Map<Long, ItemsData>> getItemsMapLiveData() {
        return itemsMapLiveData;
    }
}