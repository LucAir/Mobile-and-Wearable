package com.example.navigationbarstarter.database.item;

import com.example.navigationbarstarter.R;
import com.example.navigationbarstarter.ui.guardian.GuardianRepository;

import java.util.ArrayList;
import java.util.List;

public class InitializeItems {

    //Need to tell application to pull new item from list, when are needed
    public static final int ITEM_VERSION = 1;

    public static List<ItemsData> initializeCollectiblesForUser() {

        List<ItemsData> itemsDataList = new ArrayList<>();
        itemsDataList.addAll(initializeCharacter());
        itemsDataList.addAll(initializePet());
        return itemsDataList;
    }

    //Constructor to initialize hat
    private static List<ItemsData> initializeCharacter() {
        List<ItemsData> characterList = new ArrayList<>();

        //Adding element to the list
        return characterList;
    }

    private static List<ItemsData> initializePet() {
        List<ItemsData> petList = new ArrayList<>();

        return petList;
    }
}
