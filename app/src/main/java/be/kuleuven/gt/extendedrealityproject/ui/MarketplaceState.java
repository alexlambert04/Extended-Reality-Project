package be.kuleuven.gt.extendedrealityproject.ui;

import java.util.Collections;
import java.util.List;

public class MarketplaceState {

    private final List<MarketplaceItem> items;

    public MarketplaceState() {
        items = DummyData.getItems();
    }

    public List<MarketplaceItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
