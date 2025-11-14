package com.example.navigationbarstarter.database.guardian;

import com.example.navigationbarstarter.R;

import java.util.ArrayList;
import java.util.List;

public class InitializeItems {

    public static List<ItemsData> initializeCollectiblesForUser(int userId) {
        List<ItemsData> itemsDataList = new ArrayList<>();

        return itemsDataList;
    }

    //Constructor to initialize hat
    private static List<ItemsData> initiliazeHat() {
        List<ItemsData> hatList = new ArrayList<>();

        ItemsData noHat = new ItemsData("Regular Head", Rarity.COMMON, true, 0, Type.HAT, R.drawable.common_hat_head);


        //Adding element to the list
        hatList.add(noHat);

        return hatList;
    }

    private static List<ItemsData> initializeTshirt() {
        List<ItemsData> tshirtList = new ArrayList<>();

        //COMMON
        ItemsData stardHoodie = new ItemsData("Gray Hoodie", Rarity.COMMON, true, 0, Type.TSHIRT, R.drawable.common_tshirt_grayhoodie);
        ItemsData grayTanktop = new ItemsData("Gray Tanktop", Rarity.COMMON, false, 100,Type.TSHIRT, R.drawable.common_tshirt_grey_tanktop);
        ItemsData whiteTanktop = new ItemsData("White Tanktop", Rarity.COMMON, false, 100, Type.TSHIRT, R.drawable.common_tshirt_white_tanktop);

        //RARE
        ItemsData interTshirt = new ItemsData("Inter Tshirt", Rarity.RARE, false, 1000, Type.TSHIRT, R.drawable.rare_tshirt_inter);
        ItemsData juventusTshirt = new ItemsData("Juventus Tshirt", Rarity.RARE, false, 1000, Type.TSHIRT, R.drawable.rare_tshirt_juventus);
        ItemsData milanTshirt = new ItemsData("Milan Tshirt", Rarity.RARE, false, 1000, Type.TSHIRT, R.drawable.rare_tshirt_milan);
        ItemsData redbullTshirt = new ItemsData("Redbull Tshirt", Rarity.RARE, false, 1000, Type.TSHIRT, R.drawable.rare_tshirt_redbull);
        ItemsData mercedesTshirt = new ItemsData("Mercedes Tshirt", Rarity.RARE, false, 1000, Type.TSHIRT, R.drawable.rare_tshirt_mercedes);

        //LEGENDARY
        ItemsData superman = new ItemsData("Superman suite", Rarity.LEGENDARY, false, 5000, Type.TSHIRT, R.drawable.legendary_tshirt_superman);
        ItemsData supermario = new ItemsData("Supermario suite", Rarity.LEGENDARY, false, 5000, Type.TSHIRT, R.drawable.legendary_tshirt_supermario);

        //Adding all elements in the list
        tshirtList.add(stardHoodie);
        tshirtList.add(grayTanktop);
        tshirtList.add(whiteTanktop);
        tshirtList.add(interTshirt);
        tshirtList.add(juventusTshirt);
        tshirtList.add(milanTshirt);
        tshirtList.add(redbullTshirt);
        tshirtList.add(mercedesTshirt);

        return tshirtList;
    }

    private static List<ItemsData> initializePet() {
        List<ItemsData> petList = new ArrayList<>();

        ItemsData dragonPet = new ItemsData("Dragon", Rarity.RARE, false, 10000, Type.PET, R.drawable.rare_pet_dragon);
        return petList;
    }
}
