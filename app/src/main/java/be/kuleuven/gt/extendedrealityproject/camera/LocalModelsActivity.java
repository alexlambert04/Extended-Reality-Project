package be.kuleuven.gt.extendedrealityproject.camera;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import be.kuleuven.gt.extendedrealityproject.R;
import be.kuleuven.gt.extendedrealityproject.ar.ArViewerContract;
import be.kuleuven.gt.extendedrealityproject.databinding.ActivityLocalModelsBinding;
import be.kuleuven.gt.extendedrealityproject.supabase.MarketplaceItemRecord;
import be.kuleuven.gt.extendedrealityproject.supabase.SupabaseRepository;

public class LocalModelsActivity extends AppCompatActivity {

    private static final long MIN_CACHE_FREE_BYTES = 250L * 1024L * 1024L;

    private ActivityLocalModelsBinding binding;
    private final List<GalleryItem> items = new ArrayList<>();
    private final Set<String> expandedModelIds = new HashSet<>();
    private final Set<String> pendingDownloads = new HashSet<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
    private LocalModelsAdapter adapter;
    @Nullable
    private SupabaseRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLocalModelsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        adapter = new LocalModelsAdapter();
        binding.localModelsList.setAdapter(adapter);
        binding.refreshModelsButton.setOnClickListener(view -> loadMarketplaceItems());

        if (SupabaseRepository.isConfigured()) {
            repository = new SupabaseRepository(this);
        }

        loadMarketplaceItems();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMarketplaceItems();
    }

    @Override
    protected void onDestroy() {
        if (repository != null) {
            repository.shutdown();
        }
        super.onDestroy();
    }

    private void loadMarketplaceItems() {
        if (repository == null) {
            setEmptyState(true);
            binding.emptyStateText.setText(R.string.missing_supabase_config);
            return;
        }

        repository.fetchReadyMarketplaceItemsAsync(new SupabaseRepository.RepositoryCallback<List<MarketplaceItemRecord>>() {
            @Override
            public void onSuccess(@Nullable List<MarketplaceItemRecord> data) {
                items.clear();
                if (data != null) {
                    for (MarketplaceItemRecord record : data) {
                        String itemId = record.getId();
                        File localPly = getLocalPlyFile(itemId);
                        boolean cached = localPly.exists() && localPly.isFile() && localPly.length() > 0;
                        String title = record.getTitle();
                        if (title == null || title.trim().isEmpty()) {
                            title = itemId;
                        }
                        items.add(new GalleryItem(
                                itemId,
                                title.trim(),
                                record.getCreatedAt(),
                                record.getModelUrl(),
                                localPly,
                                cached
                        ));
                    }
                }

                Set<String> availableIds = new HashSet<>();
                for (GalleryItem item : items) {
                    availableIds.add(item.itemId);
                }
                expandedModelIds.retainAll(availableIds);
                pendingDownloads.retainAll(availableIds);

                adapter.notifyDataSetChanged();
                setEmptyState(items.isEmpty());
            }

            @Override
            public void onError(@NonNull String message, @Nullable Throwable throwable) {
                setEmptyState(true);
                binding.emptyStateText.setText(getString(R.string.marketplace_load_failed, message));
            }
        });
    }

    private void setEmptyState(boolean empty) {
        binding.emptyStateText.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.localModelsList.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    @NonNull
    private File getLocalPlyFile(@NonNull String itemId) {
        return new File(getCacheDir(), "models/" + itemId + "/model.ply");
    }

    private void onPrimaryAction(@NonNull GalleryItem item) {
        if (item.isCached) {
            openModel(item);
        } else {
            startDownload(item);
        }
    }

    private void startDownload(@NonNull GalleryItem item) {
        if (repository == null) {
            return;
        }
        if (pendingDownloads.contains(item.itemId)) {
            return;
        }
        if (item.modelUrl == null || item.modelUrl.trim().isEmpty()) {
            Toast.makeText(this, R.string.model_missing_url, Toast.LENGTH_SHORT).show();
            return;
        }
        if (getCacheDir().getUsableSpace() < MIN_CACHE_FREE_BYTES) {
            Toast.makeText(this, R.string.marketplace_not_enough_storage, Toast.LENGTH_LONG).show();
            return;
        }

        pendingDownloads.add(item.itemId);
        adapter.notifyDataSetChanged();

        repository.downloadAndExtractPlyAsync(item.itemId, item.modelUrl, new SupabaseRepository.RepositoryCallback<File>() {
            @Override
            public void onSuccess(@Nullable File data) {
                pendingDownloads.remove(item.itemId);
                if (data != null && data.exists() && data.isFile()) {
                    item.localPlyFile = data;
                    item.isCached = true;
                    Toast.makeText(LocalModelsActivity.this, R.string.marketplace_download_done, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LocalModelsActivity.this, R.string.model_extract_failed, Toast.LENGTH_SHORT).show();
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(@NonNull String message, @Nullable Throwable throwable) {
                pendingDownloads.remove(item.itemId);
                adapter.notifyDataSetChanged();
                Toast.makeText(LocalModelsActivity.this, getString(R.string.marketplace_download_failed, message), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openModel(@NonNull GalleryItem item) {
        if (!item.localPlyFile.exists() || !item.localPlyFile.isFile()) {
            Toast.makeText(this, R.string.marketplace_model_not_cached, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = ArViewerContract.createIntent(
                this,
                android.net.Uri.fromFile(item.localPlyFile),
                item.localPlyFile.getAbsolutePath(),
                item.title
        );
        startActivity(intent);
    }

    @NonNull
    private String formatModelSize(@NonNull GalleryItem item) {
        if (!item.localPlyFile.exists() || !item.localPlyFile.isFile()) {
            return getString(R.string.marketplace_not_downloaded);
        }
        double sizeMb = item.localPlyFile.length() / (1024.0 * 1024.0);
        return String.format(Locale.US, "%.2f MB", sizeMb);
    }

    @NonNull
    private String formatUploadDate(@NonNull GalleryItem item) {
        if (item.createdAt == null || item.createdAt.trim().isEmpty()) {
            return "-";
        }
        try {
            java.time.OffsetDateTime parsed = java.time.OffsetDateTime.parse(item.createdAt);
            long timeMillis = parsed.toInstant().toEpochMilli();
            return dateFormat.format(new Date(timeMillis));
        } catch (Exception ignored) {
            return item.createdAt;
        }
    }

    private void toggleExpanded(@NonNull String itemId) {
        if (expandedModelIds.contains(itemId)) {
            expandedModelIds.remove(itemId);
        } else {
            expandedModelIds.add(itemId);
        }
        adapter.notifyDataSetChanged();
    }

    private static final class GalleryItem {
        private final String itemId;
        private final String title;
        @Nullable
        private final String createdAt;
        @Nullable
        private final String modelUrl;
        private File localPlyFile;
        private boolean isCached;

        private GalleryItem(
                @NonNull String itemId,
                @NonNull String title,
                @Nullable String createdAt,
                @Nullable String modelUrl,
                @NonNull File localPlyFile,
                boolean isCached
        ) {
            this.itemId = itemId;
            this.title = title;
            this.createdAt = createdAt;
            this.modelUrl = modelUrl;
            this.localPlyFile = localPlyFile;
            this.isCached = isCached;
        }
    }

    private final class LocalModelsAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(LocalModelsActivity.this)
                        .inflate(R.layout.item_local_model, parent, false);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            GalleryItem item = items.get(position);
            boolean expanded = expandedModelIds.contains(item.itemId);
            boolean downloading = pendingDownloads.contains(item.itemId);

            holder.modelTitleText.setText(item.title);
            holder.modelIdText.setText(getString(R.string.local_models_id_value, item.itemId));
            holder.modelSizeText.setText(getString(R.string.local_models_size_value, formatModelSize(item)));
            holder.modelUploadDateText.setText(getString(R.string.local_models_upload_date_value, formatUploadDate(item)));
            holder.detailsContainer.setVisibility(expanded ? View.VISIBLE : View.GONE);
            holder.toggleExpandButton.setImageResource(
                    expanded ? android.R.drawable.arrow_up_float : android.R.drawable.arrow_down_float
            );

            holder.openModelButton.setEnabled(!downloading);
            holder.openModelButton.setAlpha(downloading ? 0.7f : 1.0f);
            holder.openModelButton.setText(item.isCached
                    ? getString(R.string.marketplace_view_model)
                    : (downloading ? getString(R.string.marketplace_downloading) : getString(R.string.marketplace_download))
            );

            if (downloading) {
                holder.modelCacheBadge.setText(R.string.marketplace_badge_downloading);
                holder.modelCacheBadge.setBackgroundResource(R.drawable.badge_state_downloading);
            } else if (item.isCached) {
                holder.modelCacheBadge.setText(R.string.marketplace_badge_cached);
                holder.modelCacheBadge.setBackgroundResource(R.drawable.badge_state_cached);
            } else {
                holder.modelCacheBadge.setText(R.string.marketplace_badge_remote);
                holder.modelCacheBadge.setBackgroundResource(R.drawable.badge_state_remote);
            }

            View.OnClickListener toggleListener = view -> toggleExpanded(item.itemId);
            holder.headerContainer.setOnClickListener(toggleListener);
            holder.toggleExpandButton.setOnClickListener(toggleListener);
            holder.openModelButton.setOnClickListener(view -> onPrimaryAction(item));

            return convertView;
        }
    }

    private static final class ViewHolder {
        private final View headerContainer;
        private final TextView modelTitleText;
        private final TextView modelIdText;
        private final TextView modelCacheBadge;
        private final ImageButton toggleExpandButton;
        private final View detailsContainer;
        private final TextView modelSizeText;
        private final TextView modelUploadDateText;
        private final Button openModelButton;

        private ViewHolder(@NonNull View root) {
            headerContainer = root.findViewById(R.id.header_container);
            modelTitleText = root.findViewById(R.id.model_title_text);
            modelIdText = root.findViewById(R.id.model_id_text);
            modelCacheBadge = root.findViewById(R.id.model_cache_badge);
            toggleExpandButton = root.findViewById(R.id.toggle_expand_button);
            detailsContainer = root.findViewById(R.id.details_container);
            modelSizeText = root.findViewById(R.id.model_size_text);
            modelUploadDateText = root.findViewById(R.id.model_upload_date_text);
            openModelButton = root.findViewById(R.id.open_model_button);
        }
    }
}
