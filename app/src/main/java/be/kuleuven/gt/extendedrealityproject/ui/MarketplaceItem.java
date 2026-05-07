package be.kuleuven.gt.extendedrealityproject.ui;

import java.util.List;

public class MarketplaceItem {

    private final String itemId;
    private final String title;
    private final Double price;
    private final String description;
    private final String sellerName;
    private final String location;
    private final String category;
    private final String modelUrl;
    private final String thumbnailUrl;
    private final List<Integer> imageResIds; // drawable resource ids for dummy data

    public MarketplaceItem(String itemId, String title, Double price, String description,
                           String sellerName, String location, String category,
                           String modelUrl, String thumbnailUrl, List<Integer> imageResIds) {
        this.itemId = itemId;
        this.title = title;
        this.price = price;
        this.description = description;
        this.sellerName = sellerName;
        this.location = location;
        this.category = category;
        this.modelUrl = modelUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.imageResIds = imageResIds;
    }

    public String getItemId() { return itemId; }
    public String getTitle() { return title; }
    public Double getPrice() { return price; }
    public String getDescription() { return description; }
    public String getSellerName() { return sellerName; }
    public String getLocation() { return location; }
    public String getCategory() { return category; }
    public String getModelUrl() { return modelUrl; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public List<Integer> getImageResIds() { return imageResIds; }
}
