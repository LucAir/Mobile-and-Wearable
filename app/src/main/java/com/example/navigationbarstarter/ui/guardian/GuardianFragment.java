package com.example.navigationbarstarter.ui.guardian;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.navigationbarstarter.R;
import com.example.navigationbarstarter.database.AppDatabase;
import com.example.navigationbarstarter.database.UserDataDao;
import com.example.navigationbarstarter.database.item.ItemsData;
import com.example.navigationbarstarter.database.item.Type;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GuardianFragment extends Fragment {

    private GuardianViewModel viewModel;

    // --- Layout Views ---
    private View bottomDrawer;
    private View btnToggleMenu;
    private ImageView imgArrow;
    private TextView tabCharacter, tabPet, tabBackground;
    private TextView tvUserTokens;
    private RecyclerView recyclerItems;
    private GuardianItemAdapter itemsAdapter;

    // --- Guardian Layers (The Preview) ---
    private ImageView layerBackground, layerAura, layerFace, layerBody, layerPet;

    // --- State Variables (Fixed Missing Variables) ---
    private boolean isMenuOpen = false;
    private Type selectedType = Type.TSHIRT; // Default to Character (Body)

    private long guardianId = -1;
    private long userId = -1;
    private long userToken = 0;

    // Helper lists/maps to manage logic
    private List<Long> unlockedItemIds = new ArrayList<>();
    private Map<Long, ItemsData> itemsMap = new HashMap<>();

    // --- Database Helpers ---
    private UserDataDao userDataDao;
    private Executor executor;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize DB access
        AppDatabase db = AppDatabase.getInstance(requireContext());
        userDataDao = db.userDataDao();
        executor = Executors.newSingleThreadExecutor();

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(GuardianViewModel.class);

        // Load User Data (ID and GuardianID) from SharedPreferences
        getUserDataFromSharedPreferences();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstance) {
        return inflater.inflate(R.layout.fragment_guardian, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Find Views
        bottomDrawer = view.findViewById(R.id.bottom_drawer);
        btnToggleMenu = view.findViewById(R.id.btn_toggle_menu);
        imgArrow = view.findViewById(R.id.img_arrow);

        tabCharacter = view.findViewById(R.id.tab_character);
        tabPet = view.findViewById(R.id.tab_pet);
        tabBackground = view.findViewById(R.id.tab_background);

        tvUserTokens = view.findViewById(R.id.tv_user_tokens);
        recyclerItems = view.findViewById(R.id.items_recycler_view);

        // Layers
        layerBackground = view.findViewById(R.id.layer_background);
        layerAura = view.findViewById(R.id.layer_aura);
        layerBody = view.findViewById(R.id.layer_body);
        layerFace = view.findViewById(R.id.layer_face);
        layerPet = view.findViewById(R.id.layer_pet);

        // 2. Setup Logic
        setupMenuToggle();
        setupTabs();
        setUpItemsGrid();

        // Load the initial category of items
        viewModel.loadItems(selectedType);

        // 3. Observe Data
        observeViewModel();

        // 4. Initial Layout State (Hide menu initially)
        view.post(() -> {
            // Get the height of the drawer
            int height = 520;

            // Move BOTH the drawer and the button down by the drawer's height
            bottomDrawer.setTranslationY(height);
            btnToggleMenu.setTranslationY(height);

            isMenuOpen = false;
        });
    }

    private void observeViewModel() {
        // 1. Observe Guardian Data (to know what is equipped)
        viewModel.getGuardianLiveData().observe(getViewLifecycleOwner(), guardian -> {
            if (guardian != null) {
                this.guardianId = guardian.getGuardianId();
                viewModel.loadEquippedItemsForGuardian(guardianId);
            }
        });

        // 2. Observe Items List (The items shown in the bottom drawer)
        viewModel.getItemsListLiveData().observe(getViewLifecycleOwner(), items -> {
            if (items != null) {
                itemsAdapter.setItems(items);
            }
        });

        // 3. Observe Unlocked Items (To show locks or allow equip)
        viewModel.getUserUnlockedItemsLiveData().observe(getViewLifecycleOwner(), unlockedIds -> {
            if (unlockedIds != null) {
                this.unlockedItemIds = unlockedIds;
                itemsAdapter.setUnlockedItemsIds(unlockedIds);
            }
        });

        // 4. Observe Equipped Items (To draw the preview characters)
        viewModel.getEquippedItemsLiveData().observe(getViewLifecycleOwner(), equippedIds -> {
            if (equippedIds != null) {
                updateGuardianPreview(equippedIds);
            }
        });

        // 5. Observe Items Map (Needed to look up images for the preview)
        viewModel.getItemsMapLiveData().observe(getViewLifecycleOwner(), map -> {
            if (map != null) {
                this.itemsMap = map;
                // If we have equipped items loaded, update preview now that map is ready
                if (viewModel.getEquippedItemsLiveData().getValue() != null) {
                    updateGuardianPreview(viewModel.getEquippedItemsLiveData().getValue());
                }
            }
        });

        // 6. Observe Loading/Error/Messages
        viewModel.getSuccessMessageLiveData().observe(getViewLifecycleOwner(), this::showSuccess);
        viewModel.getErrorLiveData().observe(getViewLifecycleOwner(), this::showError);
    }

    private void updateGuardianPreview(List<Long> equippedIds) {
        // Reset/Hide layers first
        layerFace.setVisibility(View.GONE);
        layerBody.setVisibility(View.GONE);
        layerPet.setVisibility(View.GONE);
        layerAura.setVisibility(View.GONE);
        layerBackground.setVisibility(View.GONE);

        if (itemsMap.isEmpty()) return;

        for (Long id : equippedIds) {
            ItemsData item = itemsMap.get(id);
            if (item == null) continue;

            switch (item.getType()) {
                case TSHIRT:
                    layerBody.setImageResource(item.getImageResId());
                    layerBody.setVisibility(View.VISIBLE);
                    break;
                case PET:
                    layerPet.setImageResource(item.getImageResId());
                    layerPet.setVisibility(View.VISIBLE);
                    break;
                case BACKGROUND:
                    layerBackground.setImageResource(item.getImageResId());
                    layerBackground.setVisibility(View.VISIBLE);
                    break;
            }
        }

        // Update adapter so it can show the green circle on equipped items
        itemsAdapter.setEquippedItemIds(equippedIds);
    }

    private void getUserDataFromSharedPreferences() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userId = sharedPreferences.getLong("userId", -1);

        executor.execute(() -> {
            if (userId != -1) {
                long fetchedGuardianId = userDataDao.getGuardianId(userId);
                userToken = userDataDao.getTokenNumber(userId);

                requireActivity().runOnUiThread(() -> {
                    this.guardianId = fetchedGuardianId;
                    updateTokenDisplay();
                    viewModel.loadGuardian(guardianId);
                    viewModel.loadUserUnlockedItems(userId);
                });
            }
        });
    }

    private void updateTokenDisplay() {
        if (tvUserTokens != null) {
            tvUserTokens.setText("💰 " + userToken);
        }
    }

    private void setupMenuToggle() {
        btnToggleMenu.setOnClickListener(v -> {
            // Calculate the distance (height of the drawer)
            float moveDistance = 530;

            if (isMenuOpen) {
                // --- CLOSE MENU (Go Down) ---

                // 1. Move Drawer Down
                bottomDrawer.animate()
                        .translationY(moveDistance)
                        .setDuration(300)
                        .start();

                // 2. Move Button Down (Follow the drawer)
                btnToggleMenu.animate()
                        .translationY(moveDistance)
                        .setDuration(300)
                        .start();

                // 3. Rotate Arrow back to original
                imgArrow.animate().rotation(0).setDuration(300).start();

            } else {
                // --- OPEN MENU (Go Up) ---

                // 1. Move Drawer Up (Reset to original 0 position)
                bottomDrawer.animate()
                        .translationY(0)
                        .setDuration(300)
                        .start();

                // 2. Move Button Up (Reset to original 0 position)
                btnToggleMenu.animate()
                        .translationY(0)
                        .setDuration(300)
                        .start();

                // 3. Rotate Arrow to point down
                imgArrow.animate().rotation(180).setDuration(300).start();
            }

            isMenuOpen = !isMenuOpen;
        });
    }

    private void setupTabs() {
        View.OnClickListener tabListener = v -> {
            resetTabStyles();
            ((TextView) v).setTextColor(Color.BLACK);
            ((TextView) v).setTypeface(null, Typeface.BOLD);

            int id = v.getId();
            if (id == R.id.tab_character) {
                selectedType = Type.TSHIRT;
            } else if (id == R.id.tab_pet) {
                selectedType = Type.PET;
            } else if (id == R.id.tab_background) {
                selectedType = Type.BACKGROUND;
            }
            viewModel.loadItems(selectedType);
        };

        tabCharacter.setOnClickListener(tabListener);
        tabPet.setOnClickListener(tabListener);
        tabBackground.setOnClickListener(tabListener);
    }

    private void resetTabStyles() {
        int gray = Color.parseColor("#999999");
        tabCharacter.setTextColor(gray);
        tabCharacter.setTypeface(null, Typeface.NORMAL);
        tabPet.setTextColor(gray);
        tabPet.setTypeface(null, Typeface.NORMAL);
        tabBackground.setTextColor(gray);
        tabBackground.setTypeface(null, Typeface.NORMAL);
    }

    private void setUpItemsGrid() {
        recyclerItems.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        );

        itemsAdapter = new GuardianItemAdapter(item -> handleItemClick(item));
        recyclerItems.setAdapter(itemsAdapter);
    }

    private void handleItemClick(ItemsData item) {
        boolean isUnlocked = unlockedItemIds.contains(item.getId());

        // 1. If Unlocked OR Free -> Equip
        if (isUnlocked || item.getPriceToUnlock() <= 0) {
            viewModel.equipItem(guardianId, item, null);
            return;
        }

        // 2. If Locked -> Prompt Purchase
        showPurchaseDialog(item);
    }

    private void showPurchaseDialog(ItemsData item) {
        if (userToken >= item.getPriceToUnlock()) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Unlock Item")
                    .setMessage("Buy " + item.getName() + " for " + item.getPriceToUnlock() + " tokens?")
                    .setPositiveButton("Unlock", (d, w) -> {
                        viewModel.unlockAndEquipItem(item, userToken, item.getPriceToUnlock(), guardianId, userId);
                        userToken -= item.getPriceToUnlock();
                        updateTokenDisplay();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Insufficient Tokens")
                    .setMessage("Cost: " + item.getPriceToUnlock() + "\nYou have: " + userToken)
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    private void showError(String message) {
        if (getContext() != null) Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void showSuccess(String message) {
        if (getContext() != null) Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }


}