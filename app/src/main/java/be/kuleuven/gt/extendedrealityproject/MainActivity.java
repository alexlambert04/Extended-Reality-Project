package be.kuleuven.gt.extendedrealityproject;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import be.kuleuven.gt.extendedrealityproject.camera.CameraCaptureActivity;
import be.kuleuven.gt.extendedrealityproject.camera.LocalModelsActivity;
import be.kuleuven.gt.extendedrealityproject.databinding.ActivityMainBinding;

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
}