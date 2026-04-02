package be.kuleuven.gt.extendedrealityproject.ui;

public class MarketplaceItem {

    private final String itemId;
    private final String title;

    public MarketplaceItem(String itemId, String title) {
        this.itemId = itemId;
        this.title = title;
    }

    public String getItemId() {
        return itemId;
    }

    public String getTitle() {
        return title;
    }
}

