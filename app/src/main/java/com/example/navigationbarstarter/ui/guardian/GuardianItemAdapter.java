package com.example.navigationbarstarter.ui.guardian;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.navigationbarstarter.R;
import com.example.navigationbarstarter.database.item.ItemsData;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class GuardianItemAdapter extends RecyclerView.Adapter<GuardianItemAdapter.ItemViewHolder> {

    private List<ItemsData> items = new ArrayList<>();
    private List<Long> unlockedItemIds = new ArrayList<>();
    private List<Long> equippedItemIds = new ArrayList<>();
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(ItemsData item);
        // The 'onPurchaseClick' method has been completely removed here.
    }

    public GuardianItemAdapter(OnItemClickListener listener) {
        this.listener = listener;
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
        // Using the circular layout
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_circle_guardian, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
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
        // Views from item_circle_guardian.xml
        private final MaterialCardView cardImage;
        private final ImageView itemImage;
        private final FrameLayout lockOverlay;
        private final ImageView badgeEquipped;
        private final TextView itemPrice;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            cardImage = itemView.findViewById(R.id.card_image);
            itemImage = itemView.findViewById(R.id.item_image);
            lockOverlay = itemView.findViewById(R.id.lock_overlay);
            badgeEquipped = itemView.findViewById(R.id.badge_equipped);
            itemPrice = itemView.findViewById(R.id.item_price);
        }

        public void bind(ItemsData item, boolean isUnlocked, boolean isEquipped, OnItemClickListener listener) {

            // 1. Set the specific image for this item
            itemImage.setImageResource(item.getImageResId());

            // 2. Handle Visual States
            if (isEquipped) {
                // CASE: Item is currently equipped
                badgeEquipped.setVisibility(View.VISIBLE);
                lockOverlay.setVisibility(View.GONE);
                itemPrice.setVisibility(View.GONE);

                // Green border indicating "Active"
                cardImage.setStrokeColor(Color.parseColor("#4CAF50"));
                cardImage.setStrokeWidth(6);

            } else if (isUnlocked || item.getPriceToUnlock() <= 0) {
                // CASE: Item is owned (unlocked) but not equipped
                badgeEquipped.setVisibility(View.GONE);
                lockOverlay.setVisibility(View.GONE);
                itemPrice.setVisibility(View.GONE);

                // No border
                cardImage.setStrokeColor(Color.TRANSPARENT);
                cardImage.setStrokeWidth(0);

            } else {
                // CASE: Item is locked (needs to be bought)
                badgeEquipped.setVisibility(View.GONE);
                lockOverlay.setVisibility(View.VISIBLE); // Dark overlay

                // Show the price text from the database
                itemPrice.setVisibility(View.VISIBLE);
                itemPrice.setText(String.valueOf(item.getPriceToUnlock()));

                // No border
                cardImage.setStrokeColor(Color.TRANSPARENT);
                cardImage.setStrokeWidth(0);
            }

            // 3. Click Listener
            itemView.setOnClickListener(v -> listener.onItemClick(item));
        }
    }
}