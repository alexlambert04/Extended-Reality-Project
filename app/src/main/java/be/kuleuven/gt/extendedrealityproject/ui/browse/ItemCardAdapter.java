package be.kuleuven.gt.extendedrealityproject.ui.browse;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import be.kuleuven.gt.extendedrealityproject.R;
import be.kuleuven.gt.extendedrealityproject.ui.MarketplaceItem;
import be.kuleuven.gt.extendedrealityproject.ui.detail.ItemDetailActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ItemCardAdapter extends RecyclerView.Adapter<ItemCardAdapter.ViewHolder> {

    private static final String EXTRA_ITEM_TITLE = "extra_item_title";
    private static final String EXTRA_ITEM_PRICE = "extra_item_price";
    private static final String EXTRA_ITEM_DESCRIPTION = "extra_item_description";
    private static final String EXTRA_ITEM_SELLER = "extra_item_seller";
    private static final String EXTRA_ITEM_LOCATION = "extra_item_location";
    private static final String EXTRA_ITEM_CATEGORY = "extra_item_category";
    private static final String EXTRA_ITEM_MODEL_URL = "extra_item_model_url";

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
            // Show price without decimals if round number
            double p = item.getPrice();
            if (p == Math.floor(p)) {
                price.setText(String.format(Locale.getDefault(), "€%d", (int) p));
            } else {
                price.setText(String.format(Locale.getDefault(), "€%.2f", p));
            }
            category.setText(item.getCategory());
            seller.setText(String.format("by %s", item.getSellerName()));

            if (item.getImageResIds() != null && !item.getImageResIds().isEmpty()) {
                image.setImageResource(item.getImageResIds().get(0));
            } else {
                image.setImageResource(R.drawable.placeholder_item);
            }

            itemView.setOnClickListener(v -> {
                Context ctx = v.getContext();
                Intent intent = new Intent(ctx, ItemDetailActivity.class);
                intent.putExtra(ItemDetailActivity.EXTRA_ITEM_ID, item.getItemId());
                intent.putExtra(EXTRA_ITEM_TITLE, item.getTitle());
                intent.putExtra(EXTRA_ITEM_PRICE, item.getPrice());
                intent.putExtra(EXTRA_ITEM_DESCRIPTION, item.getDescription());
                intent.putExtra(EXTRA_ITEM_SELLER, item.getSellerName());
                intent.putExtra(EXTRA_ITEM_LOCATION, item.getLocation());
                intent.putExtra(EXTRA_ITEM_CATEGORY, item.getCategory());
                intent.putExtra(EXTRA_ITEM_MODEL_URL, item.getModelUrl());
                ctx.startActivity(intent);
            });
        }
    }
}
