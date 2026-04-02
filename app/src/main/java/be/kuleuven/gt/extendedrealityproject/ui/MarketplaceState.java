package be.kuleuven.gt.extendedrealityproject.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MarketplaceState {

    private final List<MarketplaceItem> items = new ArrayList<>();

    public MarketplaceState() {
        items.add(new MarketplaceItem("demo-item-001", "Vintage Camera"));
        items.add(new MarketplaceItem("demo-item-002", "Desk Lamp"));
    }

    public List<MarketplaceItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}

