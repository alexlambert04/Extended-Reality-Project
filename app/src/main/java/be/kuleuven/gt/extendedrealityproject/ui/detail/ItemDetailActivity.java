package be.kuleuven.gt.extendedrealityproject.ui.detail;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import be.kuleuven.gt.extendedrealityproject.R;
import be.kuleuven.gt.extendedrealityproject.camera.ModelViewerActivity;
import be.kuleuven.gt.extendedrealityproject.ui.DummyData;
import be.kuleuven.gt.extendedrealityproject.ui.MarketplaceItem;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.tabs.TabLayout;

import java.util.Locale;

public class ItemDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ITEM_ID = "extra_item_id";
    public static final String EXTRA_ITEM_TITLE = "extra_item_title";
    public static final String EXTRA_ITEM_PRICE = "extra_item_price";
    public static final String EXTRA_ITEM_DESCRIPTION = "extra_item_description";
    public static final String EXTRA_ITEM_SELLER = "extra_item_seller";
    public static final String EXTRA_ITEM_LOCATION = "extra_item_location";
    public static final String EXTRA_ITEM_CATEGORY = "extra_item_category";
    public static final String EXTRA_ITEM_MODEL_URL = "extra_item_model_url";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        // Back button
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        String itemId = getIntent().getStringExtra(EXTRA_ITEM_ID);
        MarketplaceItem item = itemFromIntent(itemId);
        if (item == null) {
            item = findItem(itemId);
        }

        if (item == null) {
            finish();
            return;
        }
        final MarketplaceItem resolvedItem = item;

        // Header image
        ImageView headerImage = findViewById(R.id.detail_image);
        if (resolvedItem.getImageResIds() != null && !resolvedItem.getImageResIds().isEmpty()) {
            headerImage.setImageResource(resolvedItem.getImageResIds().get(0));
        }

        // Fields
        ((TextView) findViewById(R.id.detail_title)).setText(resolvedItem.getTitle());

        double p = resolvedItem.getPrice();
        String priceStr = (p == Math.floor(p))
                ? String.format(Locale.getDefault(), "€%d", (int) p)
                : String.format(Locale.getDefault(), "€%.2f", p);
        ((TextView) findViewById(R.id.detail_price)).setText(priceStr);

        ((TextView) findViewById(R.id.detail_description)).setText(resolvedItem.getDescription());
        ((TextView) findViewById(R.id.detail_location)).setText("📍 " + resolvedItem.getLocation());
        ((Chip) findViewById(R.id.detail_category_chip)).setText(resolvedItem.getCategory());

        // Seller
        String sellerName = resolvedItem.getSellerName();
        ((TextView) findViewById(R.id.detail_seller_name)).setText(sellerName);
        ((TextView) findViewById(R.id.detail_seller_since)).setText(getString(R.string.detail_member_since, "2024"));
        // Avatar initial
        String initial = sellerName.isEmpty() ? "?" : String.valueOf(sellerName.charAt(0)).toUpperCase(Locale.getDefault());
        ((TextView) findViewById(R.id.detail_seller_avatar)).setText(initial);

        // Tabs — Photos / 3D Model / AR View
        TabLayout tabLayout = findViewById(R.id.detail_tab_layout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                if (pos == 1) { // 3D Model
                    open3DModel(resolvedItem);
                } else if (pos == 2) { // AR View
                    open3DModel(resolvedItem);
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Buy Now
        ((MaterialButton) findViewById(R.id.btn_buy_now)).setOnClickListener(v ->
                Toast.makeText(this,
                        getString(R.string.detail_buy_stub_message, resolvedItem.getTitle()),
                        Toast.LENGTH_LONG).show()
        );

        // 3D button
        ((MaterialButton) findViewById(R.id.btn_view_ar)).setOnClickListener(v -> open3DModel(resolvedItem));

        // Contact Seller
        ((MaterialButton) findViewById(R.id.btn_contact_seller)).setOnClickListener(v ->
                Toast.makeText(this, "Contact feature coming soon!", Toast.LENGTH_SHORT).show()
        );
    }

    private void open3DModel(MarketplaceItem item) {
        if (item.getModelUrl() != null && !item.getModelUrl().isEmpty()) {
            Intent intent = new Intent(this, ModelViewerActivity.class);
            intent.putExtra(ModelViewerActivity.EXTRA_MODEL_PATH, item.getModelUrl());
            intent.putExtra(ModelViewerActivity.EXTRA_MODEL_TITLE, item.getTitle());
            startActivity(intent);
        } else {
            Toast.makeText(this, getString(R.string.detail_ar_not_available), Toast.LENGTH_SHORT).show();
        }
    }

    private MarketplaceItem findItem(String id) {
        if (id == null) return null;
        for (MarketplaceItem item : DummyData.getItems()) {
            if (item.getItemId().equals(id)) return item;
        }
        return null;
    }

    @Nullable
    private MarketplaceItem itemFromIntent(@Nullable String itemId) {
        Intent intent = getIntent();
        if (intent == null) {
            return null;
        }

        String title = trimToNull(intent.getStringExtra(EXTRA_ITEM_TITLE));
        if (itemId == null || title == null) {
            return null;
        }

        return new MarketplaceItem(
                itemId,
                title,
                intent.getDoubleExtra(EXTRA_ITEM_PRICE, 0.0),
                fallback(intent.getStringExtra(EXTRA_ITEM_DESCRIPTION), "No description available."),
                fallback(intent.getStringExtra(EXTRA_ITEM_SELLER), "Marketplace Seller"),
                fallback(intent.getStringExtra(EXTRA_ITEM_LOCATION), "Unknown location"),
                fallback(intent.getStringExtra(EXTRA_ITEM_CATEGORY), "Other"),
                trimToNull(intent.getStringExtra(EXTRA_ITEM_MODEL_URL)),
                null
        );
    }

    @Nullable
    private String trimToNull(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @NonNull
    private String fallback(@Nullable String value, @NonNull String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed == null ? defaultValue : trimmed;
    }
}
