package be.kuleuven.gt.extendedrealityproject.ui.browse;

import android.content.Context;
import android.content.Intent;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import be.kuleuven.gt.extendedrealityproject.R;
import be.kuleuven.gt.extendedrealityproject.ui.MarketplaceItem;
import be.kuleuven.gt.extendedrealityproject.ui.ImageLoader;
import be.kuleuven.gt.extendedrealityproject.ui.detail.ItemDetailActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ItemCardAdapter extends RecyclerView.Adapter<ItemCardAdapter.ViewHolder> {

    private final List<MarketplaceItem> items = new ArrayList<>();

    public void setItems(List<MarketplaceItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_marketplace_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView image;
        private final TextView title;
        private final TextView price;
        private final TextView category;
        private final TextView seller;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.item_image);
            title = itemView.findViewById(R.id.item_title);
            price = itemView.findViewById(R.id.item_price);
            category = itemView.findViewById(R.id.item_category);
            seller = itemView.findViewById(R.id.item_seller);
        }

        void bind(MarketplaceItem item) {
            title.setText(item.getTitle());
            Double p = item.getPrice();
            if (p == null) {
                price.setText("N/A");
            } else if (p == Math.floor(p)) {
                price.setText(String.format(Locale.getDefault(), "€%d", p.intValue()));
            } else {
                price.setText(String.format(Locale.getDefault(), "€%.2f", p));
            }
            category.setText(item.getCategory());
            seller.setText(String.format("by %s", item.getSellerName()));

            if (item.getThumbnailUrl() != null && !item.getThumbnailUrl().trim().isEmpty()) {
                int targetWidthPx = resolveTargetWidthPx(image);
                int targetHeightPx = dpToPx(image, 180);
                ImageLoader.loadIntoSized(image, item.getThumbnailUrl(), R.drawable.placeholder_item,
                        targetWidthPx, targetHeightPx);
            } else if (item.getImageResIds() != null && !item.getImageResIds().isEmpty()) {
                image.setImageResource(item.getImageResIds().get(0));
            } else {
                image.setImageResource(R.drawable.placeholder_item);
            }

            itemView.setOnClickListener(v -> {
                Context ctx = v.getContext();
                Intent intent = new Intent(ctx, ItemDetailActivity.class);
                intent.putExtra(ItemDetailActivity.EXTRA_ITEM_ID, item.getItemId());
                intent.putExtra(ItemDetailActivity.EXTRA_ITEM_TITLE, item.getTitle());
                if (item.getPrice() != null) {
                    intent.putExtra(ItemDetailActivity.EXTRA_ITEM_PRICE, item.getPrice());
                }
                intent.putExtra(ItemDetailActivity.EXTRA_ITEM_DESCRIPTION, item.getDescription());
                intent.putExtra(ItemDetailActivity.EXTRA_ITEM_SELLER, item.getSellerName());
                intent.putExtra(ItemDetailActivity.EXTRA_ITEM_LOCATION, item.getLocation());
                intent.putExtra(ItemDetailActivity.EXTRA_ITEM_CATEGORY, item.getCategory());
                intent.putExtra(ItemDetailActivity.EXTRA_ITEM_MODEL_URL, item.getModelUrl());
                intent.putExtra(ItemDetailActivity.EXTRA_ITEM_THUMBNAIL_URL, item.getThumbnailUrl());
                ctx.startActivity(intent);
            });
        }

        private static int resolveTargetWidthPx(@NonNull ImageView image) {
            int screenWidthPx = image.getResources().getDisplayMetrics().widthPixels;
            int horizontalPaddingPx = dpToPx(image, 24);
            int availableWidth = Math.max(1, screenWidthPx - horizontalPaddingPx);
            return Math.max(1, availableWidth / 2);
        }

        private static int dpToPx(@NonNull ImageView image, int dp) {
            return Math.round(TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    dp,
                    image.getResources().getDisplayMetrics()));
        }
    }
}
