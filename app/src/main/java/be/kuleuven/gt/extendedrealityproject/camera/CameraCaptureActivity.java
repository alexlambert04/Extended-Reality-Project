package be.kuleuven.gt.extendedrealityproject.camera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.FallbackStrategy;
import androidx.camera.video.PendingRecording;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import be.kuleuven.gt.extendedrealityproject.R;
import be.kuleuven.gt.extendedrealityproject.databinding.ActivityCameraCaptureBinding;

public class CameraCaptureActivity extends AppCompatActivity {

    private ActivityCameraCaptureBinding binding;
    private VideoCapture<Recorder> videoCapture;
    private Recording activeRecording;
    private File currentOutputFile;
    private long recordingStartTimeMs;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (activeRecording == null) {
                return;
            }
            updateTimer();
            uiHandler.postDelayed(this, 250L);
        }
    };

    private final Runnable recommendationRunnable = () -> {
        if (activeRecording != null) {
            binding.recommendationHintText.setVisibility(View.VISIBLE);
        }
    };

    private final Runnable hardStopRunnable = this::stopActiveRecording;

    private ActivityResultLauncher<String> requestCameraPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCameraCaptureBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.timerText.setText(getString(R.string.camera_timer_initial));

        requestCameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        startCamera();
                    } else {
                        Toast.makeText(this, R.string.camera_permission_needed, Toast.LENGTH_LONG).show();
                        finish();
                    }
                }
        );

        binding.recordButton.setOnClickListener(view -> {
            if (activeRecording == null) {
                startRecording();
            } else {
                stopActiveRecording();
            }
        });

        if (hasCameraPermission()) {
            startCamera();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isFinishing()) {
            stopActiveRecording();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        uiHandler.removeCallbacksAndMessages(null);
        if (activeRecording != null) {
            activeRecording.close();
            activeRecording = null;
        }
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException exception) {
                Toast.makeText(this, R.string.camera_setup_failed, Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

        Recorder recorder = new Recorder.Builder()
                .setQualitySelector(
                        QualitySelector.from(
                                Quality.FHD,
                                FallbackStrategy.lowerQualityThan(Quality.FHD)
                        )
                )
                .build();

        videoCapture = VideoCapture.withOutput(recorder);

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                videoCapture
        );
    }

    private void startRecording() {
        if (videoCapture == null) {
            Toast.makeText(this, R.string.camera_setup_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        File capturesDirectory = new File(getCacheDir(), "captures");
        if (!capturesDirectory.exists() && !capturesDirectory.mkdirs()) {
            Toast.makeText(this, R.string.recording_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        currentOutputFile = new File(capturesDirectory, "scan_" + System.currentTimeMillis() + ".mp4");
        FileOutputOptions outputOptions = new FileOutputOptions.Builder(currentOutputFile).build();
        PendingRecording pendingRecording = videoCapture.getOutput().prepareRecording(this, outputOptions);

        binding.recommendationHintText.setVisibility(View.GONE);
        recordingStartTimeMs = SystemClock.elapsedRealtime();

        activeRecording = pendingRecording.start(
                ContextCompat.getMainExecutor(this),
                this::onRecordingEvent
        );

        binding.recordButton.setText(R.string.stop_recording);
        updateTimer();
        uiHandler.post(timerRunnable);
        uiHandler.postDelayed(recommendationRunnable, RecordingFlowContract.RECOMMENDATION_MS);
        uiHandler.postDelayed(hardStopRunnable, RecordingFlowContract.MAX_RECORDING_MS);
    }

    private void stopActiveRecording() {
        if (activeRecording != null) {
            activeRecording.stop();
        }
    }

    private void onRecordingEvent(@NonNull VideoRecordEvent event) {
        if (!(event instanceof VideoRecordEvent.Finalize)) {
            return;
        }

        uiHandler.removeCallbacks(timerRunnable);
        uiHandler.removeCallbacks(recommendationRunnable);
        uiHandler.removeCallbacks(hardStopRunnable);

        if (activeRecording != null) {
            activeRecording.close();
            activeRecording = null;
        }

        binding.recordButton.setText(R.string.start_recording);

        VideoRecordEvent.Finalize finalizeEvent = (VideoRecordEvent.Finalize) event;
        if (finalizeEvent.hasError()) {
            if (currentOutputFile != null && currentOutputFile.exists()) {
                // Best effort cleanup for incomplete output.
                currentOutputFile.delete();
            }
            Toast.makeText(this, R.string.recording_failed, Toast.LENGTH_SHORT).show();
            binding.timerText.setText(getString(R.string.camera_timer_initial));
            return;
        }

        if (currentOutputFile == null || !currentOutputFile.exists()) {
            Toast.makeText(this, R.string.recording_failed, Toast.LENGTH_SHORT).show();
            binding.timerText.setText(getString(R.string.camera_timer_initial));
            return;
        }

        Intent intent = new Intent(this, RegistrationActivity.class);
        intent.putExtra(RecordingFlowContract.EXTRA_VIDEO_PATH, currentOutputFile.getAbsolutePath());
        startActivity(intent);
        finish();
    }

    private void updateTimer() {
        long elapsedMs = SystemClock.elapsedRealtime() - recordingStartTimeMs;
        long safeElapsed = Math.min(elapsedMs, RecordingFlowContract.MAX_RECORDING_MS);

        long elapsedSeconds = safeElapsed / 1000L;
        long minutes = elapsedSeconds / 60L;
        long seconds = elapsedSeconds % 60L;

        String timerText = String.format(Locale.US, "%02d:%02d / 03:00", minutes, seconds);
        binding.timerText.setText(timerText);
    }
}

