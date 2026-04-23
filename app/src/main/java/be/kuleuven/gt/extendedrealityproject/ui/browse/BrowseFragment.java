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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import be.kuleuven.gt.extendedrealityproject.R;
import be.kuleuven.gt.extendedrealityproject.ui.DummyData;
import be.kuleuven.gt.extendedrealityproject.ui.MarketplaceItem;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class BrowseFragment extends Fragment {

    private ItemCardAdapter adapter;
    private List<MarketplaceItem> allItems;
    private String currentQuery = "";
    private String currentCategory = "All Categories";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_browse, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        allItems = DummyData.getItems();

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

        applyFilter(view);
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
