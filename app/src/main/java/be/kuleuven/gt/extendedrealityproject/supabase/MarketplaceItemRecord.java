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

    public MarketplaceItemRecord(
            @NonNull String id,
            @Nullable String title,
            @NonNull PipelineStatus status,
            @Nullable String filePath,
            @Nullable String kiriSerialize,
            @Nullable String modelUrl,
            @Nullable String createdAt
    ) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.filePath = filePath;
        this.kiriSerialize = kiriSerialize;
        this.modelUrl = modelUrl;
        this.createdAt = createdAt;
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
}

