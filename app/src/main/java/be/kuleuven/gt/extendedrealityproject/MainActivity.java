package be.kuleuven.gt.extendedrealityproject;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import be.kuleuven.gt.extendedrealityproject.camera.CameraCaptureActivity;
import be.kuleuven.gt.extendedrealityproject.camera.LocalModelsActivity;
import be.kuleuven.gt.extendedrealityproject.databinding.ActivityMainBinding;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private final NativeBridge nativeBridge = new NativeBridge();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        nativeBridge.initializeRuntime();

        TextView nativeStatus = binding.nativeStatus;
        nativeStatus.setText(nativeBridge.getRuntimeStatus());

        binding.openCameraButton.setOnClickListener(view ->
                startActivity(new Intent(this, CameraCaptureActivity.class))
        );

        binding.openLocalModelsButton.setOnClickListener(view ->
                startActivity(new Intent(this, LocalModelsActivity.class))
        );

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
}