package com.example.navigationbarstarter.ui.guardian;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
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
import com.example.navigationbarstarter.database.guardian.GuardianData;
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
    private AppDatabase db;
    private UserDataDao userDataDao;
    private Executor executor;

    // --- Layout Views ---
    private View bottomDrawer;
    private View btnToggleMenu;
    private ImageView imgArrow;
    private TextView tabCharacter, tabPet, tabBackground;
    private TextView tvUserTokens;
    private TextView tvGuardianName;
    private RecyclerView recyclerItems;
    private GuardianItemAdapter itemsAdapter;

    // --- Guardian Layers (The Preview) ---
    private ImageView layerBackground, layerAura, layerFace, layerBody, layerPet;

    // --- State Variables ---
    private boolean isMenuOpen = false;
    private Type selectedType = Type.TSHIRT;

    private long guardianId = -1;
    private long userId = -1;
    private long userToken = 0;

    // Helper lists/maps
    private List<Long> unlockedItemIds = new ArrayList<>();
    private Map<Long, ItemsData> itemsMap = new HashMap<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize DB access
        db = AppDatabase.getInstance(requireContext());
        userDataDao = db.userDataDao();
        executor = Executors.newSingleThreadExecutor();

        viewModel = new ViewModelProvider(this).get(GuardianViewModel.class);
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
        tvGuardianName = view.findViewById(R.id.tv_guardian_name); // <--- FIND VIEW
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

        // Setup Name Click Listener
        if (tvGuardianName != null) {
            tvGuardianName.setOnClickListener(v -> showRenameDialog());
        }

        // Load Data
        getUserDataFromSharedPreferences();
        viewModel.loadItems(selectedType);

        // 3. Observe Data
        observeViewModel();

        // 4. Initial Layout State
        view.post(() -> {
            int height = 600;
            bottomDrawer.setTranslationY(height);
            btnToggleMenu.setTranslationY(height);

            isMenuOpen = false;
        });
    }

    private void getUserDataFromSharedPreferences() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userId = sharedPreferences.getLong("userId", -1);

        executor.execute(() -> {
            if (userId != -1) {
                long fetchedGuardianId = userDataDao.getGuardianId(userId);
                userToken = userDataDao.getTokenNumber(userId);

                // Run UI updates on Main Thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        this.guardianId = fetchedGuardianId;
                        updateTokenDisplay();

                        // Load View Model Data
                        viewModel.loadGuardian(guardianId);
                        viewModel.loadUserUnlockedItems(userId);

                        // Load Guardian Name from DB now that we have userId
                        loadGuardianNameFromDb();
                    });
                }
            }
        });
    }

    // --- NAME CUSTOMIZATION LOGIC ---

    private void showRenameDialog() {
        final EditText input = new EditText(requireContext());
        input.setSingleLine(true);
        input.setText(tvGuardianName.getText());
        input.selectAll();

        FrameLayout container = new FrameLayout(requireContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.leftMargin = 50;
        params.rightMargin = 50;
        input.setLayoutParams(params);
        container.addView(input);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Rename Guardian")
                .setMessage("Choose a name for your guardian:")
                .setView(container)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        saveGuardianName(newName);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveGuardianName(String newName) {
        if (userId == -1) return;

        executor.execute(() -> {
            GuardianData guardian = db.guardianDataDao().getGuardianById(guardianId);
            if (guardian != null) {
                guardian.setName(newName);
                db.guardianDataDao().updateGuardian(guardian);

                new Handler(Looper.getMainLooper()).post(() -> {
                    if (tvGuardianName != null) {
                        tvGuardianName.setText(newName);
                    }
                });
            }
        });
    }

    private void loadGuardianNameFromDb() {
        if (userId == -1) return;

        executor.execute(() -> {
            // We use the guardianId we fetched earlier
            GuardianData guardian = db.guardianDataDao().getGuardianById(guardianId);

            if (guardian != null && guardian.getName() != null) {
                String dbName = guardian.getName();
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (tvGuardianName != null) {
                        tvGuardianName.setText(dbName);
                    }
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
            float moveDistance = 600;
            if (isMenuOpen) {
                bottomDrawer.animate().translationY(moveDistance).setDuration(300).start();
                btnToggleMenu.animate().translationY(moveDistance).setDuration(300).start();
                imgArrow.animate().rotation(0).setDuration(300).start();
            } else {
                bottomDrawer.animate().translationY(0).setDuration(300).start();
                btnToggleMenu.animate().translationY(0).setDuration(300).start();
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
        recyclerItems.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        itemsAdapter = new GuardianItemAdapter(this::handleItemClick);
        recyclerItems.setAdapter(itemsAdapter);
    }

    private void handleItemClick(ItemsData item) {
        boolean isUnlocked = unlockedItemIds.contains(item.getId());

        if (isUnlocked || item.getPriceToUnlock() <= 0) {
            viewModel.equipItem(guardianId, item, null);
        } else {
            showPurchaseDialog(item);
        }
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

    private void observeViewModel() {
        viewModel.getGuardianLiveData().observe(getViewLifecycleOwner(), guardian -> {
            if (guardian != null) {
                this.guardianId = guardian.getGuardianId();
                viewModel.loadEquippedItemsForGuardian(guardianId);
            }
        });
        viewModel.getItemsListLiveData().observe(getViewLifecycleOwner(), items -> {
            if (items != null) itemsAdapter.setItems(items);
        });
        viewModel.getUserUnlockedItemsLiveData().observe(getViewLifecycleOwner(), unlockedIds -> {
            if (unlockedIds != null) {
                this.unlockedItemIds = unlockedIds;
                itemsAdapter.setUnlockedItemsIds(unlockedIds);
            }
        });
        viewModel.getEquippedItemsLiveData().observe(getViewLifecycleOwner(), equippedIds -> {
            if (equippedIds != null) updateGuardianPreview(equippedIds);
        });
        viewModel.getItemsMapLiveData().observe(getViewLifecycleOwner(), map -> {
            if (map != null) {
                this.itemsMap = map;
                if (viewModel.getEquippedItemsLiveData().getValue() != null) {
                    updateGuardianPreview(viewModel.getEquippedItemsLiveData().getValue());
                }
            }
        });
        viewModel.getSuccessMessageLiveData().observe(getViewLifecycleOwner(), this::showSuccess);
        viewModel.getErrorLiveData().observe(getViewLifecycleOwner(), this::showError);
    }

    private void updateGuardianPreview(List<Long> equippedIds) {
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
        itemsAdapter.setEquippedItemIds(equippedIds);
    }

    private void showError(String message) {
        if (getContext() != null) Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void showSuccess(String message) {
        if (getContext() != null) Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}