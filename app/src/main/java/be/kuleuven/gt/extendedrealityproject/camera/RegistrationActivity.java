package be.kuleuven.gt.extendedrealityproject.camera;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import be.kuleuven.gt.extendedrealityproject.MainActivity;
import be.kuleuven.gt.extendedrealityproject.R;
import be.kuleuven.gt.extendedrealityproject.databinding.ActivityRegistrationBinding;

public class RegistrationActivity extends AppCompatActivity {

    private ActivityRegistrationBinding binding;
    private String videoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegistrationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        videoPath = getIntent().getStringExtra(RecordingFlowContract.EXTRA_VIDEO_PATH);
        if (videoPath == null) {
            videoPath = "";
        }

        binding.videoPathValue.setText(videoPath);

        binding.redoRecordingButton.setOnClickListener(view -> {
            Intent redoIntent = new Intent(this, CameraCaptureActivity.class);
            startActivity(redoIntent);
            finish();
        });

        binding.generateModelButton.setOnClickListener(view -> {
            String title = binding.titleInput.getText() == null
                    ? ""
                    : binding.titleInput.getText().toString().trim();

            if (TextUtils.isEmpty(title)) {
                binding.titleInputLayout.setError(getString(R.string.title_required));
                return;
            }
            binding.titleInputLayout.setError(null);

            // Placeholder hook for Step A/B in the Supabase + KIRI flow.
            Toast.makeText(this, R.string.generation_stub_message, Toast.LENGTH_LONG).show();

            Intent doneIntent = new Intent(this, MainActivity.class);
            doneIntent.putExtra(RecordingFlowContract.EXTRA_VIDEO_PATH, videoPath);
            doneIntent.putExtra(RecordingFlowContract.EXTRA_RECORDING_TITLE, title);
            doneIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(doneIntent);
            finish();
        });
    }
}

