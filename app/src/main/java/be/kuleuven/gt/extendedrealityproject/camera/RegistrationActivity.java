package be.kuleuven.gt.extendedrealityproject.camera;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.Locale;

import be.kuleuven.gt.extendedrealityproject.MainActivity;
import be.kuleuven.gt.extendedrealityproject.R;
import be.kuleuven.gt.extendedrealityproject.databinding.ActivityRegistrationBinding;
import be.kuleuven.gt.extendedrealityproject.supabase.MarketplaceItemRecord;
import be.kuleuven.gt.extendedrealityproject.supabase.SupabaseRepository;

public class RegistrationActivity extends AppCompatActivity {

    private ActivityRegistrationBinding binding;
    private String videoPath;
    private SupabaseRepository repository;
    private boolean creditsExhausted;
    private boolean loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegistrationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new SupabaseRepository(this);

        videoPath = getIntent().getStringExtra(RecordingFlowContract.EXTRA_VIDEO_PATH);
        if (videoPath == null) {
            videoPath = "";
        }

        binding.videoPathValue.setText(buildRecordedFileText(videoPath));
        refreshCreditsStatus();

        binding.redoRecordingButton.setOnClickListener(view -> {
            Intent redoIntent = new Intent(this, CameraCaptureActivity.class);
            startActivity(redoIntent);
            finish();
        });

        binding.generateModelButton.setOnClickListener(view -> {
            if (creditsExhausted) {
                Toast.makeText(this, R.string.generation_limit_reached_message, Toast.LENGTH_LONG).show();
                return;
            }

            String title = binding.titleInput.getText() == null
                    ? ""
                    : binding.titleInput.getText().toString().trim();

            if (TextUtils.isEmpty(title)) {
                binding.titleInputLayout.setError(getString(R.string.title_required));
                return;
            }
            binding.titleInputLayout.setError(null);

            File recordedFile = new File(videoPath);
            if (!recordedFile.exists()) {
                Toast.makeText(this, R.string.video_file_missing, Toast.LENGTH_LONG).show();
                return;
            }

            long fileSizeBytes = recordedFile.length();
            if (fileSizeBytes > RecordingFlowContract.MAX_UPLOAD_BYTES) {
                String readableSize = formatBytes(fileSizeBytes);
                new AlertDialog.Builder(this)
                        .setTitle(R.string.video_too_large_title)
                        .setMessage(getString(R.string.video_too_large, readableSize))
                        .setPositiveButton(R.string.video_too_large_action_redo, (dialog, which) -> openRedoWithShorterHint())
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                return;
            }

            if (!SupabaseRepository.isConfigured()) {
                Toast.makeText(this, R.string.missing_supabase_config, Toast.LENGTH_LONG).show();
                return;
            }

            setLoading(true);
            repository.createAndStartGeneration(title, videoPath, new SupabaseRepository.RepositoryCallback<MarketplaceItemRecord>() {
                @Override
                public void onSuccess(MarketplaceItemRecord data) {
                    setLoading(false);
                    if (data == null) {
                        Toast.makeText(RegistrationActivity.this, R.string.generation_kickoff_failed, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Intent sellIntent = new Intent(RegistrationActivity.this, MainActivity.class);
                    sellIntent.putExtra(RecordingFlowContract.EXTRA_ITEM_ID, data.getId());
                    sellIntent.putExtra(RecordingFlowContract.EXTRA_RECORDING_TITLE, title);
                    sellIntent.putExtra(RecordingFlowContract.EXTRA_VIDEO_PATH, videoPath);
                    startActivity(sellIntent);
                    finish();
                }

                @Override
                public void onError(String message, Throwable throwable) {
                    setLoading(false);
                    if (SupabaseRepository.isCreditsExhaustedError(message, throwable)) {
                        creditsExhausted = true;
                        updateCreditsUi(0);
                        Toast.makeText(RegistrationActivity.this, R.string.generation_limit_reached_message, Toast.LENGTH_LONG).show();
                        return;
                    }
                    Toast.makeText(RegistrationActivity.this, getString(R.string.generation_kickoff_failed) + " " + message, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCreditsStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        repository.shutdown();
    }

    private void setLoading(boolean loading) {
        this.loading = loading;
        binding.generateModelButton.setEnabled(!loading && !creditsExhausted);
        binding.redoRecordingButton.setEnabled(!loading);
        binding.titleInput.setEnabled(!loading);
        binding.generateModelButton.setVisibility(View.VISIBLE);
    }

    private void refreshCreditsStatus() {
        if (!SupabaseRepository.isConfigured()) {
            creditsExhausted = false;
            updateCreditsUi(-1);
            return;
        }

        repository.fetchAvailableScansAsync(new SupabaseRepository.RepositoryCallback<Integer>() {
            @Override
            public void onSuccess(Integer data) {
                int count = data == null ? 0 : Math.max(0, data);
                creditsExhausted = count <= 0;
                updateCreditsUi(count);
            }

            @Override
            public void onError(String message, Throwable throwable) {
                creditsExhausted = false;
                updateCreditsUi(-1);
            }
        });
    }

    private void updateCreditsUi(int count) {
        if (count >= 0) {
            binding.availableScansLabel.setText(getString(R.string.available_scans_value, count));
        } else {
            binding.availableScansLabel.setText(getString(R.string.available_scans_unknown));
        }

        binding.generationLimitWarning.setVisibility(creditsExhausted ? View.VISIBLE : View.GONE);
        binding.generateModelButton.setEnabled(!loading && !creditsExhausted);
    }

    private String buildRecordedFileText(String path) {
        if (TextUtils.isEmpty(path)) {
            return "";
        }

        File recordedFile = new File(path);
        if (!recordedFile.exists()) {
            return path;
        }

        String sizeText = getString(R.string.recorded_video_size_label, formatBytes(recordedFile.length()));
        return path + "\n" + sizeText;
    }

    private String formatBytes(long bytes) {
        double sizeMb = bytes / (1024.0 * 1024.0);
        return String.format(Locale.US, "%.1f MB", sizeMb);
    }

    private void openRedoWithShorterHint() {
        Intent redoIntent = new Intent(this, CameraCaptureActivity.class);
        redoIntent.putExtra(RecordingFlowContract.EXTRA_HINT_SHORTER_RECORDING, true);
        startActivity(redoIntent);
        finish();
    }
}
