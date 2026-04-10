package be.kuleuven.gt.extendedrealityproject.camera;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import be.kuleuven.gt.extendedrealityproject.MainActivity;
import be.kuleuven.gt.extendedrealityproject.R;
import be.kuleuven.gt.extendedrealityproject.databinding.ActivityGenerationProgressBinding;
import be.kuleuven.gt.extendedrealityproject.supabase.GenerationSessionStore;
import be.kuleuven.gt.extendedrealityproject.supabase.MarketplaceItemRecord;
import be.kuleuven.gt.extendedrealityproject.supabase.PipelineStatus;
import be.kuleuven.gt.extendedrealityproject.supabase.SupabaseRealtimeClient;
import be.kuleuven.gt.extendedrealityproject.supabase.SupabaseRepository;

public class GenerationProgressActivity extends AppCompatActivity {

    private static final long POLL_INTERVAL_MS = 10_000L;

    private ActivityGenerationProgressBinding binding;
    private SupabaseRepository repository;
    private SupabaseRealtimeClient realtimeClient;
    private GenerationSessionStore sessionStore;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private String itemId;
    private String title;
    private String videoPath;

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            fetchLatest();
            uiHandler.postDelayed(this, POLL_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGenerationProgressBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new SupabaseRepository(this);
        sessionStore = new GenerationSessionStore(this);

        itemId = getIntent().getStringExtra(RecordingFlowContract.EXTRA_ITEM_ID);
        title = getIntent().getStringExtra(RecordingFlowContract.EXTRA_RECORDING_TITLE);
        videoPath = getIntent().getStringExtra(RecordingFlowContract.EXTRA_VIDEO_PATH);

        if (itemId == null || itemId.trim().isEmpty()) {
            GenerationSessionStore.Session session = sessionStore.load();
            if (session != null) {
                itemId = session.itemId;
                title = session.title;
                videoPath = session.videoPath;
            }
        }

        if (itemId == null || itemId.trim().isEmpty()) {
            Toast.makeText(this, R.string.generation_kickoff_failed, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        sessionStore.save(itemId, title, videoPath);

        binding.retryButton.setOnClickListener(view -> retryStartJob());
        binding.backHomeButton.setOnClickListener(view -> backHome());

        attachRealtime();
        fetchLatest();
        uiHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        uiHandler.removeCallbacks(pollRunnable);
        if (realtimeClient != null) {
            realtimeClient.disconnect();
        }
        repository.shutdown();
    }

    private void attachRealtime() {
        realtimeClient = new SupabaseRealtimeClient(
                repository.getRealtimeWebsocketUrl(),
                repository.getAnonKey(),
                new SupabaseRealtimeClient.RealtimeListener() {
                    @Override
                    public void onItemUpdate(@NonNull MarketplaceItemRecord item, @NonNull String rawPayload) {
                        render(item, "Realtime\n" + rawPayload);
                    }

                    @Override
                    public void onInfo(@NonNull String message) {
                        appendInfo(message);
                        if (message.startsWith("Realtime error") || message.startsWith("Realtime closed")) {
                            appendInfo(getString(R.string.generation_realtime_disconnected));
                        }
                    }
                }
        );
        realtimeClient.connect(itemId);
    }

    private void fetchLatest() {
        repository.fetchMarketplaceItemAsync(itemId, new SupabaseRepository.RepositoryCallback<MarketplaceItemRecord>() {
            @Override
            public void onSuccess(MarketplaceItemRecord data) {
                if (data != null) {
                    render(data, "Polling refresh");
                }
            }

            @Override
            public void onError(String message, Throwable throwable) {
                appendInfo("Polling error: " + message);
            }
        });
    }

    private void retryStartJob() {
        repository.retryStartKiriJobAsync(itemId, new SupabaseRepository.RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                Toast.makeText(GenerationProgressActivity.this, R.string.generation_retry_done, Toast.LENGTH_SHORT).show();
                fetchLatest();
            }

            @Override
            public void onError(String message, Throwable throwable) {
                Toast.makeText(GenerationProgressActivity.this, R.string.generation_retry_failed, Toast.LENGTH_SHORT).show();
                appendInfo("Retry error: " + message);
            }
        });
    }

    private void render(@NonNull MarketplaceItemRecord item, @NonNull String source) {
        String userStage = mapStage(item.getStatus());
        binding.progressStageValue.setText(userStage);

        String updatedAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
        StringBuilder debug = new StringBuilder();
        debug.append(String.format(Locale.US, getString(R.string.generation_item_id), item.getId())).append("\n");
        debug.append("Title: ").append(safe(item.getTitle())).append("\n");
        debug.append("Raw status: ").append(item.getStatus().name()).append("\n");
        debug.append(String.format(Locale.US, getString(R.string.generation_last_update), updatedAt)).append("\n");
        debug.append(String.format(Locale.US, getString(R.string.generation_serialize), safe(item.getKiriSerialize()))).append("\n");
        debug.append(String.format(Locale.US, getString(R.string.generation_model_url), safe(item.getModelUrl()))).append("\n");
        debug.append("Source: ").append(source).append("\n");
        debug.append("Created at: ").append(safe(item.getCreatedAt())).append("\n");
        debug.append("File path: ").append(safe(item.getFilePath()));

        binding.debugText.setText(debug.toString());

        if (item.getStatus().isTerminal()) {
            uiHandler.removeCallbacks(pollRunnable);
            if (item.getStatus() == PipelineStatus.READY) {
                sessionStore.clear();
            }
        }
    }

    private void appendInfo(@NonNull String text) {
        CharSequence current = binding.debugText.getText();
        String combined = current + "\n" + text;
        binding.debugText.setText(combined.trim());
    }

    private String mapStage(@NonNull PipelineStatus status) {
        switch (status) {
            case UPLOADING:
                return getString(R.string.generation_status_uploading);
            case SENDING_TO_KIRI:
                return getString(R.string.generation_status_sending_to_kiri);
            case PROCESSING_IN_CLOUD:
                return getString(R.string.generation_status_processing);
            case DOWNLOADING_ARTIFACT:
                return getString(R.string.generation_status_downloading);
            case READY:
                return getString(R.string.generation_status_ready);
            case FAILED:
                return getString(R.string.generation_status_failed);
            case UNKNOWN:
            default:
                return getString(R.string.generation_status_unknown);
        }
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    private void backHome() {
        Intent homeIntent = new Intent(this, MainActivity.class);
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(homeIntent);
        finish();
    }
}

