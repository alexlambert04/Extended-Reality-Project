package be.kuleuven.gt.extendedrealityproject;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import be.kuleuven.gt.extendedrealityproject.ar.ArViewerContract;
import be.kuleuven.gt.extendedrealityproject.camera.CameraCaptureActivity;
import be.kuleuven.gt.extendedrealityproject.camera.LocalModelsActivity;
import be.kuleuven.gt.extendedrealityproject.databinding.ActivityMainBinding;
import be.kuleuven.gt.extendedrealityproject.supabase.SupabaseRepository;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MainActivity extends AppCompatActivity {

    private static final String QUICK_AR_ASSET_PATH = "models/bottle.glb";

    private ActivityMainBinding binding;
    private final NativeBridge nativeBridge = new NativeBridge();
    private SupabaseRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        nativeBridge.initializeRuntime();
        repository = new SupabaseRepository(this);

        TextView nativeStatus = binding.nativeStatus;
        nativeStatus.setText(nativeBridge.getRuntimeStatus());

        binding.openCameraButton.setOnClickListener(view ->
                startActivity(new Intent(this, CameraCaptureActivity.class))
        );

        binding.openLocalModelsButton.setOnClickListener(view ->
                startActivity(new Intent(this, LocalModelsActivity.class))
        );

        binding.openQuickArButton.setOnClickListener(view -> openBundledArDemo());

        binding.openCameraInfoButton.setOnClickListener(view ->
                showFeatureInfoDialog(R.string.main_record_info_title, R.string.main_record_info_message)
        );

        binding.openLocalModelsInfoButton.setOnClickListener(view ->
                showFeatureInfoDialog(R.string.main_local_models_info_title, R.string.main_local_models_info_message)
        );

        binding.capturePoseButton.setOnClickListener(view -> {
            float[] dummyPose = new float[16];
            dummyPose[0] = 1.0f;
            dummyPose[5] = 1.0f;
            dummyPose[10] = 1.0f;
            dummyPose[15] = 1.0f;
            nativeBridge.submitCameraPose(dummyPose);
        });

        binding.startTrainingButton.setOnClickListener(view -> {
            nativeBridge.startTraining("demo-item-001");
            nativeStatus.setText(nativeBridge.getRuntimeStatus());
        });

        binding.stopTrainingButton.setOnClickListener(view -> {
            nativeBridge.stopTraining();
            nativeStatus.setText(nativeBridge.getRuntimeStatus());
        });

        refreshAvailableScans();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAvailableScans();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (repository != null) {
            repository.shutdown();
        }
    }

    private void refreshAvailableScans() {
        if (!SupabaseRepository.isConfigured()) {
            binding.availableScansValue.setText(getString(R.string.available_scans_unknown));
            return;
        }

        repository.fetchAvailableScansAsync(new SupabaseRepository.RepositoryCallback<Integer>() {
            @Override
            public void onSuccess(Integer data) {
                int count = data == null ? 0 : Math.max(0, data);
                binding.availableScansValue.setText(getString(R.string.available_scans_value, count));
                boolean canRecord = count > 0;
                binding.openCameraButton.setEnabled(canRecord);
                binding.scanAvailabilityHint.setText(canRecord
                        ? R.string.available_scans_ready_hint
                        : R.string.generation_limit_reached_message);
            }

            @Override
            public void onError(String message, Throwable throwable) {
                binding.availableScansValue.setText(getString(R.string.available_scans_unknown));
                binding.scanAvailabilityHint.setText(R.string.available_scans_fetch_failed);
                if (BuildConfig.DEBUG) {
                    Toast.makeText(MainActivity.this, "Credits fetch failed: " + message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void showFeatureInfoDialog(@StringRes int titleRes, @StringRes int messageRes) {
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(titleRes)
                .setMessage(messageRes)
                .setPositiveButton(R.string.info_dialog_close, null)
                .create();

        dialog.setOnShowListener(d -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundBlurRadius(28);
            }
        });

        dialog.show();
    }

    private void openBundledArDemo() {
        try {
            File source = ensureBundledDemoInCache();
            Intent intent = ArViewerContract.createIntent(
                    this,
                    android.net.Uri.fromFile(source),
                    source.getAbsolutePath(),
                    getString(R.string.main_quick_ar_button)
            );
            startActivity(intent);
        } catch (IOException exception) {
            Toast.makeText(this, R.string.main_quick_ar_missing_asset, Toast.LENGTH_LONG).show();
        }
    }

    private File ensureBundledDemoInCache() throws IOException {
        File cacheDir = new File(getCacheDir(), "ar-demo");
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            throw new IOException("Could not create AR demo cache directory");
        }

        File outFile = new File(cacheDir, "bottle.glb");
        if (outFile.exists() && outFile.length() > 0) {
            return outFile;
        }

        try (InputStream in = getAssets().open(QUICK_AR_ASSET_PATH);
             FileOutputStream out = new FileOutputStream(outFile, false)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.getFD().sync();
        }

        if (!outFile.exists() || outFile.length() == 0) {
            throw new IOException("AR demo file is empty");
        }
        return outFile;
    }
}