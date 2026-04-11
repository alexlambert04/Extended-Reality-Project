package be.kuleuven.gt.extendedrealityproject.camera;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import be.kuleuven.gt.extendedrealityproject.databinding.ActivityLocalModelsBinding;

public class LocalModelsActivity extends AppCompatActivity {

    private ActivityLocalModelsBinding binding;
    private final List<LocalModel> models = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLocalModelsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.refreshModelsButton.setOnClickListener(view -> loadModels());
        binding.localModelsList.setOnItemClickListener((parent, view, position, id) -> openModel(models.get(position)));

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
                    models.add(new LocalModel(folder.getName(), modelFile));
                }
            }
        }

        Collections.sort(models, Comparator.comparingLong((LocalModel model) -> model.file.lastModified()).reversed());

        List<String> rows = new ArrayList<>();
        for (LocalModel model : models) {
            rows.add(formatRow(model));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                rows
        );
        binding.localModelsList.setAdapter(adapter);

        boolean empty = rows.isEmpty();
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
    private String formatRow(@NonNull LocalModel model) {
        double sizeMb = model.file.length() / (1024.0 * 1024.0);
        String updated = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(model.file.lastModified()));
        return model.itemId + "\n" + String.format(Locale.US, "%.2f MB | Updated %s", sizeMb, updated);
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
        private final File file;

        private LocalModel(@NonNull String itemId, @NonNull File file) {
            this.itemId = itemId;
            this.file = file;
        }
    }
}
