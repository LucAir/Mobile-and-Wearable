package com.example.navigationbarstarter.database.item;

import com.example.navigationbarstarter.R;
import java.util.ArrayList;
import java.util.List;


public class InitializeItems {

    public static final int ITEM_VERSION = 1;

    public static List<ItemsData> initializeCollectiblesForUser() {
        List<ItemsData> items = new ArrayList<>();

        // --- CHARACTER SKINS (Type.TSHIRT) ---

        items.add(new ItemsData(
                "Inter Jersey",
                Rarity.COMMON,
                0,  // Price
                Type.TSHIRT,
                R.drawable.classic_suit
        ));

        items.add(new ItemsData(
                "Inter Jersey",
                Rarity.RARE,
                200,
                Type.TSHIRT,
                R.drawable.inter_suit
        ));

        items.add(new ItemsData(
                "Rome Jersey",
                Rarity.RARE,
                200,
                Type.TSHIRT,
                R.drawable.rome_suit
                ));


        items.add(new ItemsData(
                "Superman Suit",
                Rarity.LEGENDARY,
                1000,
                Type.TSHIRT,
                R.drawable.superman_suit
        ));

        items.add(new ItemsData(
                "Super-mario Suit",
                Rarity.LEGENDARY,
                1500,
                Type.TSHIRT,
                R.drawable.supermario_suit
        ));



        // --- PETS (Type.PET) ---

        items.add(new ItemsData(
                "Dragon",
                Rarity.COMMON,
                0,
                Type.PET,
                R.drawable.classic_pet
        ));

        items.add(new ItemsData(
                "Inter Snake",
                Rarity.RARE,
                120,
                Type.PET,
                R.drawable.inter_pet
        ));

        items.add(new ItemsData(
                "Superman dog",
                Rarity.LEGENDARY,
                3000,
                Type.PET,
                R.drawable.superman_pet
        ));

        items.add(new ItemsData(
                "Toad",
                Rarity.LEGENDARY,
                1500,
                Type.PET,
                R.drawable.supermario_pet
        ));



        // --- BACKGROUNDS (Type.BACKGROUND) ---

        items.add(new ItemsData(
                "Guardian Library",
                Rarity.COMMON,
                0,
                Type.BACKGROUND,
                R.drawable.classic_background
        ));

        items.add(new ItemsData(
                "San Siro",
                Rarity.RARE,
                180,
                Type.BACKGROUND,
                R.drawable.inter_background
        ));

        items.add(new ItemsData(
                "Rome Jersey",
                Rarity.RARE,
                180,
                Type.BACKGROUND,
                R.drawable.rome_background
        ));


        items.add(new ItemsData(
                "Krypton",
                Rarity.LEGENDARY,
                1500,
                Type.BACKGROUND,
                R.drawable.superman_background
        ));

        items.add(new ItemsData(
                "Super-mario level",
                Rarity.LEGENDARY,
                3000,
                Type.BACKGROUND,
                R.drawable.supermario_background
        ));

        return items;
    }
}