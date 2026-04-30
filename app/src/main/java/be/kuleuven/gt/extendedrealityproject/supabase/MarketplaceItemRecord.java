package be.kuleuven.gt.extendedrealityproject.supabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class MarketplaceItemRecord {

    private final String id;
    private final String title;
    private final PipelineStatus status;
    private final String filePath;
    private final String kiriSerialize;
    private final String modelUrl;
    private final String createdAt;
    private final String usedApiKeyId;
    private final String sellerName;
    private final String location;
    private final String category;
    private final String description;
    private final Double price;

    public MarketplaceItemRecord(
            @NonNull String id,
            @Nullable String title,
            @NonNull PipelineStatus status,
            @Nullable String filePath,
            @Nullable String kiriSerialize,
            @Nullable String modelUrl,
            @Nullable String createdAt,
            @Nullable String usedApiKeyId,
            @Nullable String sellerName,
            @Nullable String location,
            @Nullable String category,
            @Nullable String description,
            @Nullable Double price
    ) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.filePath = filePath;
        this.kiriSerialize = kiriSerialize;
        this.modelUrl = modelUrl;
        this.createdAt = createdAt;
        this.usedApiKeyId = usedApiKeyId;
        this.sellerName = sellerName;
        this.location = location;
        this.category = category;
        this.description = description;
        this.price = price;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    @NonNull
    public PipelineStatus getStatus() {
        return status;
    }

    @Nullable
    public String getFilePath() {
        return filePath;
    }

    @Nullable
    public String getKiriSerialize() {
        return kiriSerialize;
    }

    @Nullable
    public String getModelUrl() {
        return modelUrl;
    }

    @Nullable
    public String getCreatedAt() {
        return createdAt;
    }

    @Nullable
    public String getUsedApiKeyId() {
        return usedApiKeyId;
    }

    @Nullable
    public String getSellerName() {
        return sellerName;
    }

    @Nullable
    public String getLocation() {
        return location;
    }

    @Nullable
    public String getCategory() {
        return category;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    @Nullable
    public Double getPrice() {
        return price;
    }
}
