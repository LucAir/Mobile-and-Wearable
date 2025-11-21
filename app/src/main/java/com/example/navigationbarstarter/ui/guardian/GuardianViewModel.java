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

    /*HOW IT WORKS
        - Use MutableLiveData to post results from background thread
        - MutableLiveData notifies the fragment whenever data changes
            - DB loads data and ViewModel updateLiveData
            - Fragment observers update UI automatically
            -> no manual refresh -> less bug (in theory :))
            - UI updates only when fragment is visible

        - WORKFLOW:
            - Fragment ask ViewModel for data
            - Fragment observes LiveData
            - UI gets updated
                - we get all the component of the guardian -> then draw it in the UI
     */

    private final GuardianRepository guardianRepository;
    private AppDatabase database;
    private Executor executor;

    private final MutableLiveData<GuardianData> guardianLive = new MutableLiveData<>();
    private final MutableLiveData<List<ItemsData>> itemsLive = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>();
    private final MutableLiveData<String> message = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<List<ItemsData>> itemsListLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Long>> equippedItemsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Long>> userUnlockedItemsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Map<Long, ItemsData>> itemsMapLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<ItemsData>> allItemsLiveData = new MutableLiveData<>();

    public GuardianViewModel(@NonNull Application application) {
        super(application);
        database = AppDatabase.getInstance(application.getApplicationContext());
        guardianRepository = new GuardianRepository(application.getApplicationContext());
        executor = Executors.newSingleThreadExecutor();
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
            loading.postValue(false);
            if  (onComplete != null) {
                onComplete.run();
                message.postValue("Equipped " + item.getName());
            }
        });
    }

    //Load the guardian from DB and posts the result to guardianLive for the fragment to observe
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
        return itemsListLiveData;
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

    public MutableLiveData<Boolean> getLoadingLiveData() {
        return loading;
    }

    //TODO: check if it is correct -> surely needs to be loaded somewhere (put in the variable a value)
    public MutableLiveData<String> getErrorLiveData() {
        return error;
    }

    public MutableLiveData<String> getSuccessMessageLiveData() {
        return message;
    }



    //ViewModel to perform unlocking (deduct token & add to unlocked list) and then equip
    public void unlockAndEquipItem(ItemsData item, long userToken, long priceTokenItem, long guardianId, long userId) {
        guardianRepository.unlockAndEquipItem(item, userToken, priceTokenItem, guardianId, userId);
    }

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
