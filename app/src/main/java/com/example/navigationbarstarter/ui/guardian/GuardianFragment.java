package com.example.navigationbarstarter.ui.guardian;

import static com.example.navigationbarstarter.database.item.Type.AURA;
import static com.example.navigationbarstarter.database.item.Type.BACKGROUND;
import static com.example.navigationbarstarter.database.item.Type.HAT;
import static com.example.navigationbarstarter.database.item.Type.PET;
import static com.example.navigationbarstarter.database.item.Type.TSHIRT;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.navigationbarstarter.R;
import com.example.navigationbarstarter.database.AppDatabase;
import com.example.navigationbarstarter.database.UserDataDao;
import com.example.navigationbarstarter.database.guardian.GuardianData;
import com.example.navigationbarstarter.database.guardian.GuardianDataDao;
import com.example.navigationbarstarter.database.item.ItemsData;
import com.example.navigationbarstarter.database.item.ItemsDataDao;
import com.example.navigationbarstarter.database.item.Rarity;
import com.example.navigationbarstarter.database.item.Type;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GuardianFragment extends Fragment {

    /**
     * GuardianFragment - complete fragment that:
     *  - displays the guardian layered (background/body/tshirt/hat/aura/pet)
     *  - shows a right sidebar of categories
     *  - shows bottom grid of items for the selected category
     *  - lets the user equip or buy items (via ViewModel)
     */

    private GuardianViewModel viewModel;

    //Views -> guardian layered
    private MaterialButton btnBackground, btnBody, btnTshirt, btnHat, btnAura, btnPet;

    private TextView txtCategoryTitle;
    private ProgressBar progressBar;

    //Log user token
    private TextView tvUserTokens;

    //Rarity filter
    private ChipGroup chipGroupRarity;
    private Chip chipAll, chipCommon, chipRare, chipLegendary;

    /**
     * RecyclerView -> view that allow to display a dynamic list (create elements when necessary)
     * Also handles when items go out of the screen due to length of the list or just the scroll of it
     * Every item of the list is inside a view Holder.
     * ViewHolder -> is a wrap around a view that contains the layout for a single element in the list
     * Adapter -> creates viewHolder object and set data for this view
     * When we define an adapter we need to override 3 methods:
     *  1)onCreateViewHolder -> called by ReciclerView whenever we need to create a new viewHolder
     *                          Create the viewHolder and the corresponding view, but does not bind data
     *  2)onBindViewHolder -> link data to the previously created viewHolder
     *  3)getItemCount -> used to get dimension of the dataset -> used to determine when no more element
     *                    can be visualized
     *
     *
     */
    private RecyclerView recyclerItems;
    private GuardianItemAdapter itemsAdapter;

    //Instantiate reference to database
    AppDatabase db;
    Executor executor;
    ItemsDataDao itemsDataDao;
    GuardianDataDao guardianDataDao;
    UserDataDao userDataDao;

    //Simple state
    private Type selectedType = HAT;
    private Rarity selectedRarity = null;
    private long userId;
    private long guardianId;
    private long userToken;

    //Categories in order to display on the right side bar
    private final List<Type> CATEGORY_ORDER = Arrays.asList(
            HAT,
            TSHIRT,
            PET,
            AURA,
            BACKGROUND
    );

    private Map<Long, ItemsData> itemsMap = new HashMap<>();
    private List<Long> unlockedItemIds = new ArrayList<>();

    public GuardianFragment() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Initialize values for db
        initialize(requireContext());

        //Get User data from shared preferences
        getUserDataFromSharedPreferences();

        //Initialize viewModel
        viewModel = new ViewModelProvider(this).get(GuardianViewModel.class);
    }

    //Inflates the UI layout
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstance) {
        return inflater.inflate(R.layout.fragment_guardian, container, false);
    }

    /**
     * - Initialize UI views
     * - Configures RecyclerView and Tabs
     * - Subscribes to LiveData
     * - Starts loading guardian data from DB
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //Find views
        tvUserTokens = view.findViewById(R.id.tv_user_tokens);
        recyclerItems = view.findViewById(R.id.items_recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);

        //Category buttons
        btnHat = view.findViewById(R.id.btn_hat);
        btnTshirt = view.findViewById(R.id.btn_tshirt);
        btnAura = view.findViewById(R.id.btn_aura);
        btnBackground = view.findViewById(R.id.btn_background);
        btnPet = view.findViewById(R.id.btn_pet);

        //Rarity chips
        chipGroupRarity = view.findViewById(R.id.chip_group_rarity);
        chipAll = view.findViewById(R.id.chip_all);
        chipCommon = view.findViewById(R.id.chip_common);
        chipRare = view.findViewById(R.id.chip_rare);
        chipLegendary = view.findViewById(R.id.chip_legendary);

        //SetUp everything
        setupCategoryButtons();
        setupRarityChips();
        setUpItemsGrid();
        observeViewModel();

        //Loading variables
        viewModel.loadGuardian(guardianId);
        viewModel.loadItems(selectedType);

        //Load user token
        loadUserToken();
    }

    private void initialize(Context context) {
        db = AppDatabase.getInstance(context);
        itemsDataDao = db.itemsDataDao();
        guardianDataDao = db.guardianDataDao();
        userDataDao = db.userDataDao();
        executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Used to retrieve information of the user from the shared preferences
     * We retrieve userId, guardianId and userToken
     */
    private void getUserDataFromSharedPreferences() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userId = sharedPreferences.getLong("userId", -1);

        executor.execute(() -> {
            if (userId != -1) {
                guardianId = userDataDao.getGuardianId(userId);
                userToken = userDataDao.getTokenNumber(userId);

                //Update UI on main thread
                requireActivity().runOnUiThread(this::updateTokenDisplay);
            }
        });
    }

    /**
     * Update UI relative to the visualization of the number of user token
     */
    private void updateTokenDisplay() {
        if (tvUserTokens != null) {
            tvUserTokens.setText(String.valueOf(userToken));
        }
    }

    private void setupCategoryButtons() {
        btnHat.setOnClickListener(v -> onCategorySelected(HAT));
        btnTshirt.setOnClickListener(v -> onCategorySelected(TSHIRT));
        btnAura.setOnClickListener(v -> onCategorySelected(AURA));
        btnBackground.setOnClickListener(v -> onCategorySelected(BACKGROUND));
        btnPet.setOnClickListener(v -> onCategorySelected(PET));

        // Set initial selected state
        updateCategoryButtonStates();
    }

    private void onCategorySelected(Type type) {
        selectedType = type;
        updateCategoryButtonStates();
        viewModel.loadItems(type);
    }

    private void updateCategoryButtonStates() {
        // Reset all buttons
        btnHat.setBackgroundColor(selectedType == HAT ? 0xFFE0E0E0 : 0x00000000);
        btnTshirt.setBackgroundColor(selectedType == TSHIRT ? 0xFFE0E0E0 : 0x00000000);
        btnAura.setBackgroundColor(selectedType == AURA ? 0xFFE0E0E0 : 0x00000000);
        btnBackground.setBackgroundColor(selectedType == BACKGROUND ? 0xFFE0E0E0 : 0x00000000);
        btnPet.setBackgroundColor(selectedType == PET ? 0xFFE0E0E0 : 0x00000000);
    }

    private void setupRarityChips() {
        chipGroupRarity.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                selectedRarity = null;
            } else {
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chip_all) {
                    selectedRarity = null;
                } else if (checkedId == R.id.chip_common) {
                    selectedRarity = Rarity.COMMON;
                } else if (checkedId == R.id.chip_rare) {
                    selectedRarity = Rarity.RARE;
                } else if (checkedId == R.id.chip_legendary) {
                    selectedRarity = Rarity.LEGENDARY;
                }
            }
            filterItems();
        });
    }

    private void filterItems() {
        List<ItemsData> currentItems = viewModel.getItemsListLiveData().getValue();
        if (currentItems != null) {
            filterAndDisplayItems(currentItems);
        }
    }

    private void filterAndDisplayItems(List<ItemsData> items) {
        if (selectedRarity == null) {
            itemsAdapter.setItems(items);
        } else {
            List<ItemsData> filtered = new ArrayList<>();
            for (ItemsData item : items) {
                if (item.getRarity() == selectedRarity) {
                    filtered.add(item);
                }
            }
            itemsAdapter.setItems(filtered);
        }
    }

    private void loadUserToken() {
        executor.execute(() -> {
            if (userId != -1) {
                userToken = userDataDao.getTokenNumber(userId);
                requireActivity().runOnUiThread(this::updateTokenDisplay);
            }
        });
    }

    private void setUpItemsGrid() {
        itemsAdapter = new GuardianItemAdapter(new GuardianItemAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(ItemsData item) {
                showItemDetails(item);
            }

            @Override
            public void onPurchaseClick(ItemsData item) {
                onItemActionClicked(item);
            }
        });
        recyclerItems.setAdapter(itemsAdapter);
        recyclerItems.setLayoutManager(new GridLayoutManager(requireContext(), 2));
    }

    //Observers
    private void observeViewModel() {
        //Guardian entity (contains equipped ids)
        viewModel.getGuardianLiveData().observe(getViewLifecycleOwner(), guardian -> {
            if (guardian == null) return;
            viewModel.loadEquippedItemsForGuardian(guardian.getGuardianId());
        });

        //Items map (all items by ID)
        viewModel.getItemsMapLiveData().observe(getViewLifecycleOwner(), map -> {
            this.itemsMap = map;
        });

        //Equipped items
        viewModel.getEquippedItemsLiveData().observe(getViewLifecycleOwner(), equippedItems -> {
            //viewModel should return a list that contains items or nulls in known order, or map by type
            //We'll try to set images by matching their type
            if (equippedItems != null) {
                itemsAdapter.setEquippedItemIds(equippedItems);
            }
        });

        //Item lists for current category TODO: check if current category
        viewModel.getItemsListLiveData().observe(getViewLifecycleOwner(), items -> {
           if (items!= null) {
               filterAndDisplayItems(items);
           }
        });

        //User unlocked items
        viewModel.getUserUnlockedItemsLiveData().observe(getViewLifecycleOwner(), unlockedItemIds -> {
            if (unlockedItemIds != null) {
                this.unlockedItemIds = unlockedItemIds;
                itemsAdapter.setUnlockedItemsIds(unlockedItemIds);
            }
        });

        //Loading state
        viewModel.getLoadingLiveData().observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE);
        });

        //Error messages
        viewModel.getErrorLiveData().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });

        //Success messages
        viewModel.getSuccessMessageLiveData().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    //Item click handling
    private void onItemActionClicked(final ItemsData item) {
        List<Long> unlocked = unlockedItemIds;
        boolean isUnlocked = unlocked.contains(item.getId());

        GuardianData guardian = viewModel.getGuardianLiveData().getValue();
        if (guardian == null) {
            Toast.makeText(requireContext(), "No guardian loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        //Check if already equipped
        boolean alreadyEquipped = isAlreadyEquipped(item, guardian);
        if (alreadyEquipped) {
            Toast.makeText(requireContext(), item.getName() + " is already equipped", Toast.LENGTH_SHORT).show();
            return;
        }

        //If unlocked -> equip
        if (isUnlocked) {
            viewModel.equipItem(guardianId, item, null);
            Toast.makeText(requireContext(), "Equipped: " + item.getName(), Toast.LENGTH_SHORT).show();
            return;
        }

        //If not unlocked and free -> equip
        if (item.getPriceToUnlock() <= 0) {
            viewModel.equipItem(guardianId, item, null);
            Toast.makeText(requireContext(), "Equipped: " + item.getName(), Toast.LENGTH_SHORT).show();
            return;
        }

        //If user can afford -> unlock and equip
        if (item.getPriceToUnlock() <= userToken) {
            showPurchaseConfirmation(item);
            return;
        }

        //Cannot afford
        showCannotAffordDialog(item);
    }

    private boolean isAlreadyEquipped(ItemsData item, GuardianData guardian) {
        switch (item.getType()) {
            case HAT: return guardian.getEquippedHat() == item.getId();
            case TSHIRT: return guardian.getEquippedTshirt() == item.getId();
            case PET: return guardian.getEquippedPet() == item.getId();
            case AURA: return guardian.getEquippedAura() == item.getId();
            case BACKGROUND: return guardian.getEquippedBackground() == item.getId();
            default: return false;
        }
    }

    private void showItemDetails(ItemsData item) {
        boolean isUnlocked = unlockedItemIds.contains(item.getId());

        String message = "📦 " + item.getName() + "\n\n"
                + "Rarity: " + item.getRarity() + "\n"
                + "Type: " + item.getType() + "\n"
                + (isUnlocked ? "✓ Unlocked" : "🔒 Locked - Cost: " + item.getPriceToUnlock() + " tokens");

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Item Details")
                .setMessage(message)
                .setPositiveButton("OK", (d, w) -> d.dismiss())
                .show();
    }

    private void showPurchaseConfirmation(ItemsData item) {
        String message = "Purchase " + item.getName() + " for " + item.getPriceToUnlock() + " tokens?";

        new MaterialAlertDialogBuilder(requireContext()).setTitle("Confirm Purchase")
                .setMessage(message)
                .setPositiveButton("Buy", (d, w) -> {
                    viewModel.unlockAndEquipItem(item, userToken, item.getPriceToUnlock(), guardianId, userId);
                    userToken -= item.getPriceToUnlock();
                    updateTokenDisplay();
                })
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .show();
    }

    private void showCannotAffordDialog(ItemsData item) {
        long need = Math.max(0, item.getPriceToUnlock() - userToken);
        String message = "🔒 " + item.getName() + "\n\n"
                + "Rarity: " + item.getRarity() + "\n"
                + "Cost: " + item.getPriceToUnlock() + " XP\n"
                + "You have: " + userToken + " userToken\n\n"
                + "You need " + need + " more userToken!";
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Not Enough userToken")
                .setMessage(message)
                .setPositiveButton("OK", (d, w) -> d.dismiss())
                .show();
    }

    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_SHORT).show();
        }
    }

    private void showSuccess(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), "Success: " + message, Toast.LENGTH_SHORT).show();
        }
    }

    //---------------------------------------------------------------------
    //CREATING THE RECYCLER VIEW ADAPTER OVERRIDING THE 3 METHODS
    //---------------------------------------------------------------------
    public class GuardianItemAdapter extends RecyclerView.Adapter<GuardianItemAdapter.ItemViewHolder> {
        private List<ItemsData> items = new ArrayList<>();
        private List<Long> unlockedItemIds = new ArrayList<>();
        private List<Long> equippedItemIds = new ArrayList<>();
        private final OnItemClickListener listener;

        public interface OnItemClickListener {
            void onItemClick(ItemsData item);
            void onPurchaseClick(ItemsData item);
        }

        public GuardianItemAdapter(OnItemClickListener lister) {
            this.listener = lister;
        }

        public void setItems(List<ItemsData> items) {
            this.items = items != null ? items : new ArrayList<>();
            notifyDataSetChanged();
        }

        public void setUnlockedItemsIds(List<Long> ids) {
            this.unlockedItemIds = ids != null ? ids : new ArrayList<>();
            notifyDataSetChanged();
        }

        public void setEquippedItemIds(List<Long> ids) {
            this.equippedItemIds = ids != null ? ids : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_guardian, parent, false);
            return new ItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull GuardianItemAdapter.ItemViewHolder holder, int position) {
            ItemsData item = items.get(position);
            boolean isUnlocked = unlockedItemIds.contains(item.getId());
            boolean isEquipped = equippedItemIds.contains(item.getId());
            holder.bind(item, isUnlocked, isEquipped, listener);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ItemViewHolder extends RecyclerView.ViewHolder {
            private final ImageView itemImage;
            private final TextView itemName;
            private final TextView itemPrice;
            private final TextView itemRarity;
            private final View rarityIndicator;
            private final MaterialButton btnAction;
            private final TextView badgeEquipped;
            private final View lockOverlay;
            private final TextView iconLocked;

            public ItemViewHolder(@NonNull View itemView) {
                super(itemView);
                itemImage = itemView.findViewById(R.id.item_image);
                itemName = itemView.findViewById(R.id.item_name);
                itemPrice = itemView.findViewById(R.id.item_price);
                itemRarity = itemView.findViewById(R.id.item_rarity);
                rarityIndicator = itemView.findViewById(R.id.rarity_indicator);
                btnAction = itemView.findViewById(R.id.btn_action);
                badgeEquipped = itemView.findViewById(R.id.badge_equipped);
                lockOverlay = itemView.findViewById(R.id.lock_overlay);
                iconLocked = itemView.findViewById(R.id.icon_locked);
            }

            public void bind(ItemsData item,
                             boolean isUnlocked,
                             boolean isEquipped,
                             OnItemClickListener listener) {
                itemName.setText(item.getName());
                itemPrice.setText(String.valueOf(item.getPriceToUnlock()));
                itemRarity.setText(item.getRarity().toString());
                itemImage.setImageResource(item.getImageResId());

                //Set rarity color
                int color = getRarityColor(item.getRarity());
                rarityIndicator.setBackgroundColor(color);

                //Handle equipped badge
                if (isEquipped) {
                    badgeEquipped.setVisibility(View.VISIBLE);
                    btnAction.setText("Equipped");
                    btnAction.setEnabled(false);
                } else if (isUnlocked) {
                    badgeEquipped.setVisibility(View.GONE);
                    btnAction.setText("Equip");
                    btnAction.setEnabled(true);
                } else {
                    badgeEquipped.setVisibility(View.GONE);
                    btnAction.setText("Buy");
                    btnAction.setEnabled(true);
                }

                //Handle lock overlay
                if (!isUnlocked && item.getPriceToUnlock() > 0) {
                    lockOverlay.setVisibility(View.VISIBLE);
                    iconLocked.setVisibility(View.VISIBLE);
                } else {
                    lockOverlay.setVisibility(View.GONE);
                    iconLocked.setVisibility(View.GONE);
                }

                itemView.setOnClickListener(v -> listener.onItemClick(item));
                btnAction.setOnClickListener(v -> listener.onPurchaseClick(item));
            }

            private int getRarityColor(Rarity rarity) {
                switch (rarity) {
                    case COMMON:
                        return Color.parseColor("#9E9E9E");
                    case RARE:
                        return Color.parseColor("#42A5F5");
                    case LEGENDARY:
                        return Color.parseColor("#FFD700");
                    default:
                        return Color.GRAY;
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        tvUserTokens = null;
        recyclerItems = null;
    }
}

