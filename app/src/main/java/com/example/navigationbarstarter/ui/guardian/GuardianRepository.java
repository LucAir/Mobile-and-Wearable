package com.example.navigationbarstarter.ui.guardian;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.util.Log;

import com.example.navigationbarstarter.database.AppDatabase;
import com.example.navigationbarstarter.database.UserData;
import com.example.navigationbarstarter.database.UserDataDao;
import com.example.navigationbarstarter.database.guardian.GuardianData;
import com.example.navigationbarstarter.database.guardian.GuardianDataDao;
import com.example.navigationbarstarter.database.item.InitializeItems;
import com.example.navigationbarstarter.database.item.ItemsData;
import com.example.navigationbarstarter.database.item.ItemsDataDao;
import com.example.navigationbarstarter.database.item.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GuardianRepository {

    /*ROLE:
        - this is a layer located in the middle between db and the main thread
        - uses Executor to run db calls in the background (not on the main thread)
        - we pass callback to have a way to send results on the UI thread after background execution completes
     */
    private final AppDatabase db;
    private final ItemsDataDao itemsDataDao;
    private final GuardianDataDao guardianDataDao;
    private final UserDataDao userDataDao;
    private final Executor executor = Executors.newSingleThreadExecutor();

    public GuardianRepository(Context context) {
        db = AppDatabase.getInstance(context);
        itemsDataDao = db.itemsDataDao();
        guardianDataDao = db.guardianDataDao();
        userDataDao = db.userDataDao();
    }

    public void insertGuardian(final GuardianData guardianData, final CallbackLong callback) {
        executor.execute(() -> {
            long id = guardianDataDao.insert(guardianData);
            if (callback != null) callback.onResult(id);
        });
    }

    public void loadItemsByType(final Type type, final CallbackListItems callbackListItems) {
        executor.execute(() -> {
            List<ItemsData> items = itemsDataDao.getItemsByType(type);
            callbackListItems.onResults(items);
        });
    }

    //Used to update the guardian
    public void equipItem(final long guardianId, final ItemsData item, final Runnable onDone) {
        executor.execute(() -> {
            GuardianData guardianData = guardianDataDao.getGuardianById(guardianId);
            if (guardianData == null) return;
            applyEquip(guardianData, item);

            guardianDataDao.updateGuardian(guardianData);
            if (onDone != null) {
                onDone.run();
            }
        });
    }

    public void unlockAndEquipItem(ItemsData item, long userToken, long priceTokenItem, long guardianId, long userId) {
        executor.execute(() -> {
            //Update user token and unlocked items
            UserData userData = db.userDataDao().getUserById(userId);
            long currentUserToken = userToken - priceTokenItem;
            List<Long> unlockedItems = userData.getUnlockedItems();
            unlockedItems.add(item.getId());
            userData.setUnlockedItems(unlockedItems);
            userData.setToken(currentUserToken);
            db.userDataDao().updateUser(userData);

            //Updating guardian (adding the object to the used one)
            GuardianData guardianData = db.guardianDataDao().getGuardianById(guardianId);
            applyEquip(guardianData, item);

            db.guardianDataDao().updateGuardian(guardianData);
        });
    }

    public void loadEquippedItems(long guardianId, CallbackListLong callback) {
        executor.execute(() -> {

            GuardianData gd = db.guardianDataDao().getGuardianById(guardianId);
            List<Long> equipped = (gd != null) ? gd.getEquippedItems() : new ArrayList<>();

            callback.onResult(equipped);
        });
    }

    private void applyEquip(GuardianData guardianData, ItemsData itemsData) {
        switch (itemsData.getType()) {

            case TSHIRT:
                guardianData.setEquippedTshirt(itemsData.getId());
                break;

            case PET:
                guardianData.setEquippedPet(itemsData.getId());
                break;

            case BACKGROUND:
                guardianData.setEquippedBackground(itemsData.getId());
                break;
        }
    }

    public List<ItemsData> getAllItems() {
        return db.itemsDataDao().getAllItems();
    }

    public interface CallbackLong {
        void onResult(long id);
    }

    public interface CallbackListLong {
        void onResult(List<Long> id);
    }

    public interface CallbackListItems {
        void onResults(List<ItemsData> items);
    }
}
