package be.kuleuven.gt.extendedrealityproject.ui.browse;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import be.kuleuven.gt.extendedrealityproject.R;
import be.kuleuven.gt.extendedrealityproject.ui.DummyData;
import be.kuleuven.gt.extendedrealityproject.ui.MarketplaceItem;
import be.kuleuven.gt.extendedrealityproject.supabase.MarketplaceItemRecord;
import be.kuleuven.gt.extendedrealityproject.supabase.SupabaseRepository;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Collections;

public class BrowseFragment extends Fragment {

    private ItemCardAdapter adapter;
    private List<MarketplaceItem> allItems;
    private String currentQuery = "";
    private String currentCategory = "All Categories";
    @Nullable
    private SupabaseRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_browse, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        allItems = new ArrayList<>();
        if (SupabaseRepository.isConfigured()) {
            repository = new SupabaseRepository(requireContext());
        }

        // 2-column grid
        RecyclerView recycler = view.findViewById(R.id.recycler_items);
        recycler.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        adapter = new ItemCardAdapter();
        recycler.setAdapter(adapter);

        // Search
        TextInputEditText searchInput = view.findViewById(R.id.search_input);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                currentQuery = s.toString().trim().toLowerCase();
                applyFilter(view);
            }
        });

        // Category dropdown
        String[] categories = { "All Categories", "Furniture", "Electronics", "Sports", "Clothing", "Other" };
        AutoCompleteTextView catDropdown = view.findViewById(R.id.dropdown_category);
        catDropdown.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, categories));
        catDropdown.setOnItemClickListener((parent, v, position, id) -> {
            currentCategory = categories[position];
            applyFilter(view);
        });

        // Sort dropdown (stub)
        String[] sorts = { "Most Recent", "Price: Low to High", "Price: High to Low" };
        AutoCompleteTextView sortDropdown = view.findViewById(R.id.dropdown_sort);
        sortDropdown.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, sorts));

        loadItems(view);
    }

    @Override
    public void onDestroy() {
        if (repository != null) {
            repository.shutdown();
        }
        super.onDestroy();
    }

    private void loadItems(@NonNull View root) {
        if (repository == null) {
            allItems = DummyData.getItems();
            updateProcessingBanner(root, null);
            applyFilter(root);
            return;
        }

        loadProcessingItems(root);

        repository.fetchReadyMarketplaceItemsAsync(new SupabaseRepository.RepositoryCallback<List<MarketplaceItemRecord>>() {
            @Override
            public void onSuccess(@Nullable List<MarketplaceItemRecord> data) {
                if (!isAdded()) {
                    return;
                }
                if (data == null || data.isEmpty()) {
                    allItems = new ArrayList<>();
                } else {
                    allItems = mapRecordsToMarketplaceItems(data);
                }
                applyFilter(root);
            }

            @Override
            public void onError(@NonNull String message, @Nullable Throwable throwable) {
                if (!isAdded()) {
                    return;
                }
                allItems = DummyData.getItems();
                applyFilter(root);
                Toast.makeText(requireContext(), getString(R.string.marketplace_load_failed, message), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadProcessingItems(@NonNull View root) {
        if (repository == null) {
            updateProcessingBanner(root, null);
            return;
        }

        repository.fetchProcessingMarketplaceItemsAsync(new SupabaseRepository.RepositoryCallback<List<MarketplaceItemRecord>>() {
            @Override
            public void onSuccess(@Nullable List<MarketplaceItemRecord> data) {
                if (!isAdded()) {
                    return;
                }
                updateProcessingBanner(root, data);
            }

            @Override
            public void onError(@NonNull String message, @Nullable Throwable throwable) {
                if (!isAdded()) {
                    return;
                }
                updateProcessingBanner(root, null);
            }
        });
    }

    private void updateProcessingBanner(@NonNull View root, @Nullable List<MarketplaceItemRecord> records) {
        View container = root.findViewById(R.id.processing_container);
        if (container == null) {
            return;
        }

        if (records == null || records.isEmpty()) {
            container.setVisibility(View.GONE);
            return;
        }

        TextView titleView = root.findViewById(R.id.processing_title);
        TextView descriptionView = root.findViewById(R.id.processing_description);
        TextView listView = root.findViewById(R.id.processing_list);

        titleView.setText(getString(R.string.browse_processing_title, records.size()));
        descriptionView.setText(getString(R.string.browse_processing_description));
        listView.setText(buildProcessingList(records));
        container.setVisibility(View.VISIBLE);
    }

    @NonNull
    private String buildProcessingList(@NonNull List<MarketplaceItemRecord> records) {
        int maxItems = 3;
        int total = records.size();
        int count = Math.min(total, maxItems);

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            MarketplaceItemRecord record = records.get(i);
            String title = record.getTitle();
            if (title == null || title.trim().isEmpty()) {
                title = getString(R.string.browse_processing_item_fallback, shortId(record.getId()));
            } else {
                title = title.trim();
            }

            if (i > 0) {
                builder.append("\n");
            }
            builder.append("- ").append(title);
        }

        if (total > maxItems) {
            builder.append("\n");
            builder.append(getString(R.string.browse_processing_more, total - maxItems));
        }

        return builder.toString();
    }

    @NonNull
    private String shortId(@NonNull String itemId) {
        if (itemId.length() <= 6) {
            return itemId;
        }
        return itemId.substring(0, 6);
    }

    @NonNull
    private List<MarketplaceItem> mapRecordsToMarketplaceItems(@NonNull List<MarketplaceItemRecord> records) {
        List<MarketplaceItem> mapped = new ArrayList<>();
        for (MarketplaceItemRecord record : records) {
            String itemId = record.getId();
            String title = safeText(record.getTitle(), itemId);
            String category = safeText(record.getCategory(), "Other");
            String seller = safeText(record.getSellerName(), mockSellerName(itemId));
            String location = safeText(record.getLocation(), "Belgium");
            String description = safeText(record.getDescription(),
                    "3D model generated from Supabase pipeline. Additional listing details are mocked for now.");
            Double price = record.getPrice();
            String modelUrl = nullIfBlank(record.getModelUrl());
            String thumbnailUrl = nullIfBlank(record.getThumbnailUrl());

            mapped.add(new MarketplaceItem(
                    itemId,
                    title,
                    price,
                    description,
                    seller,
                    location,
                    category,
                    modelUrl,
                    thumbnailUrl,
                    Collections.singletonList(R.drawable.placeholder_item)
            ));
        }
        return mapped;
    }

    @NonNull
    private String safeText(@Nullable String value, @NonNull String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    @Nullable
    private String nullIfBlank(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    @NonNull
    private String mockSellerName(@NonNull String itemId) {
        int seed = Math.abs(itemId.hashCode());
        return String.format(Locale.US, "Seller %03d", (seed % 900) + 100);
    }

    private double mockPrice(@NonNull String itemId) {
        int seed = Math.abs(itemId.hashCode());
        int euros = 50 + (seed % 350);
        return (double) euros;
    }

    private void applyFilter(View root) {
        List<MarketplaceItem> filtered = new ArrayList<>();
        for (MarketplaceItem item : allItems) {
            boolean matchesCategory = currentCategory.equals("All Categories")
                    || item.getCategory().equalsIgnoreCase(currentCategory);
            boolean matchesQuery = currentQuery.isEmpty()
                    || item.getTitle().toLowerCase().contains(currentQuery)
                    || item.getDescription().toLowerCase().contains(currentQuery)
                    || item.getLocation().toLowerCase().contains(currentQuery);
            if (matchesCategory && matchesQuery) {
                filtered.add(item);
            }
        }
        adapter.setItems(filtered);

        TextView countLabel = root.findViewById(R.id.items_count_label);
        countLabel.setText(getString(R.string.browse_items_count, filtered.size()));
    }
}
