package be.kuleuven.gt.extendedrealityproject.camera;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import be.kuleuven.gt.extendedrealityproject.R;
import be.kuleuven.gt.extendedrealityproject.databinding.ActivityLocalModelsBinding;

public class LocalModelsActivity extends AppCompatActivity {

    private ActivityLocalModelsBinding binding;
    private final List<LocalModel> models = new ArrayList<>();
    private final Set<String> expandedModelIds = new HashSet<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
    private LocalModelsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLocalModelsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.refreshModelsButton.setOnClickListener(view -> loadModels());
        adapter = new LocalModelsAdapter();
        binding.localModelsList.setAdapter(adapter);

        loadModels();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadModels();
    }

    private void loadModels() {
        models.clear();

        File modelsRoot = new File(getCacheDir(), "models");
        File[] itemFolders = modelsRoot.listFiles(File::isDirectory);
        if (itemFolders != null) {
            for (File folder : itemFolders) {
                File modelFile = chooseModelFile(folder);
                if (modelFile != null && modelFile.exists() && modelFile.isFile() && modelFile.length() > 0) {
                    String itemId = folder.getName();
                    String title = readTitleFromMetadata(folder, itemId);
                    models.add(new LocalModel(itemId, title, modelFile));
                }
            }
        }

        Collections.sort(models, Comparator.comparingLong((LocalModel model) -> model.file.lastModified()).reversed());

        Set<String> availableIds = new HashSet<>();
        for (LocalModel model : models) {
            availableIds.add(model.itemId);
        }
        expandedModelIds.retainAll(availableIds);

        adapter.notifyDataSetChanged();

        boolean empty = models.isEmpty();
        binding.emptyStateText.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.localModelsList.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void openModel(@NonNull LocalModel model) {
        Intent intent = new Intent(this, ModelViewerActivity.class);
        intent.putExtra(ModelViewerActivity.EXTRA_MODEL_PATH, model.file.getAbsolutePath());
        intent.putExtra(ModelViewerActivity.EXTRA_MODEL_TITLE, model.itemId);
        startActivity(intent);
    }

    @NonNull
    private String formatModelSize(@NonNull LocalModel model) {
        double sizeMb = model.file.length() / (1024.0 * 1024.0);
        return String.format(Locale.US, "%.2f MB", sizeMb);
    }

    @NonNull
    private String formatUploadDate(@NonNull LocalModel model) {
        return dateFormat.format(new Date(model.file.lastModified()));
    }

    private void toggleExpanded(@NonNull String itemId) {
        if (expandedModelIds.contains(itemId)) {
            expandedModelIds.remove(itemId);
        } else {
            expandedModelIds.add(itemId);
        }
        adapter.notifyDataSetChanged();
    }

    @NonNull
    private String readTitleFromMetadata(@NonNull File modelFolder, @NonNull String fallbackId) {
        File metadataFile = new File(modelFolder, "metadata.json");
        if (metadataFile.exists() && metadataFile.isFile()) {
            try {
                byte[] bytes = java.nio.file.Files.readAllBytes(metadataFile.toPath());
                JSONObject jsonObject = new JSONObject(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
                String title = jsonObject.optString("title", "").trim();
                if (!title.isEmpty()) {
                    return title;
                }
            } catch (IOException | JSONException ignored) {
                // Use folder id as fallback if metadata is missing or invalid.
            }
        }
        return fallbackId;
    }

    @Nullable
    private File chooseModelFile(@NonNull File folder) {
        File splat = new File(folder, "model.splat");
        if (splat.exists()) {
            return splat;
        }

        File splatv = new File(folder, "model.splatv");
        if (splatv.exists()) {
            return splatv;
        }

        File ply = new File(folder, "model.ply");
        if (ply.exists()) {
            return ply;
        }

        return null;
    }

    private static final class LocalModel {
        private final String itemId;
        private final String title;
        private final File file;

        private LocalModel(@NonNull String itemId, @NonNull String title, @NonNull File file) {
            this.itemId = itemId;
            this.title = title;
            this.file = file;
        }
    }

    private final class LocalModelsAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return models.size();
        }

        @Override
        public Object getItem(int position) {
            return models.get(position);
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

            LocalModel model = models.get(position);
            boolean expanded = expandedModelIds.contains(model.itemId);

            holder.modelTitleText.setText(model.title);
            holder.modelIdText.setText(getString(R.string.local_models_id_value, model.itemId));
            holder.modelSizeText.setText(getString(R.string.local_models_size_value, formatModelSize(model)));
            holder.modelUploadDateText.setText(getString(R.string.local_models_upload_date_value, formatUploadDate(model)));
            holder.detailsContainer.setVisibility(expanded ? View.VISIBLE : View.GONE);
            holder.toggleExpandButton.setImageResource(
                    expanded ? android.R.drawable.arrow_up_float : android.R.drawable.arrow_down_float
            );

            View.OnClickListener toggleListener = view -> toggleExpanded(model.itemId);
            holder.headerContainer.setOnClickListener(toggleListener);
            holder.toggleExpandButton.setOnClickListener(toggleListener);
            holder.openModelButton.setOnClickListener(view -> openModel(model));

            return convertView;
        }
    }

    private static final class ViewHolder {
        private final View headerContainer;
        private final TextView modelTitleText;
        private final TextView modelIdText;
        private final ImageButton toggleExpandButton;
        private final View detailsContainer;
        private final TextView modelSizeText;
        private final TextView modelUploadDateText;
        private final View openModelButton;

        private ViewHolder(@NonNull View root) {
            headerContainer = root.findViewById(R.id.header_container);
            modelTitleText = root.findViewById(R.id.model_title_text);
            modelIdText = root.findViewById(R.id.model_id_text);
            toggleExpandButton = root.findViewById(R.id.toggle_expand_button);
            detailsContainer = root.findViewById(R.id.details_container);
            modelSizeText = root.findViewById(R.id.model_size_text);
            modelUploadDateText = root.findViewById(R.id.model_upload_date_text);
            openModelButton = root.findViewById(R.id.open_model_button);
        }
    }
}
