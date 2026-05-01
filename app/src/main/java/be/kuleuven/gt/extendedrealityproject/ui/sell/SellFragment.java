package be.kuleuven.gt.extendedrealityproject.ui.sell;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.util.Objects;

import be.kuleuven.gt.extendedrealityproject.R;
import be.kuleuven.gt.extendedrealityproject.camera.CameraCaptureActivity;
import be.kuleuven.gt.extendedrealityproject.supabase.SupabaseRepository;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class SellFragment extends Fragment {

    public static final String ARG_ITEM_ID = "arg_item_id";
    public static final String ARG_PREFILL_TITLE = "arg_prefill_title";
    public static final String ARG_VIDEO_PATH = "arg_video_path";

    @Nullable
    private SupabaseRepository repository;
    @Nullable
    private String pendingItemId;

    private Uri selectedVideoUri = null;
    private Uri selectedThumbnailUri = null;
    @Nullable
    private File selectedThumbnailFile = null;

    private TextView videoFileNameLabel;
    private TextView videoPlaceholderLabel;
    private TextView thumbnailPlaceholderLabel;
    private TextView thumbnailFileNameLabel;
    private View thumbnailPlaceholder;
    private ImageView thumbnailPreview;

    private ActivityResultLauncher<String> requestThumbnailPermissionLauncher;

    private final ActivityResultLauncher<Uri> takeThumbnailLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (!success || selectedThumbnailFile == null) {
                    Toast.makeText(requireContext(), R.string.sell_thumbnail_capture_failed, Toast.LENGTH_SHORT).show();
                    return;
                }

                thumbnailPreview.setImageBitmap(BitmapFactory.decodeFile(selectedThumbnailFile.getAbsolutePath()));
                thumbnailPreview.setVisibility(View.VISIBLE);
                thumbnailPlaceholder.setVisibility(View.GONE);
                thumbnailFileNameLabel.setText(getString(R.string.sell_thumbnail_file_name, selectedThumbnailFile.getName()));
                thumbnailFileNameLabel.setVisibility(View.VISIBLE);
                thumbnailPlaceholderLabel.setText(getString(R.string.sell_thumbnail_selected));
            });

    private final ActivityResultLauncher<String> pickVideoLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedVideoUri = uri;
                    String name = uri.getLastPathSegment();
                    videoFileNameLabel.setText(name != null ? name : uri.toString());
                    videoFileNameLabel.setVisibility(View.VISIBLE);
                    videoPlaceholderLabel.setText(getString(R.string.sell_video_selected));
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sell, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        videoFileNameLabel = view.findViewById(R.id.video_file_name_label);
        videoPlaceholderLabel = view.findViewById(R.id.video_placeholder_label);
        thumbnailPlaceholderLabel = view.findViewById(R.id.thumbnail_placeholder_label);
        thumbnailFileNameLabel = view.findViewById(R.id.thumbnail_file_name_label);
        thumbnailPlaceholder = view.findViewById(R.id.thumbnail_placeholder);
        thumbnailPreview = view.findViewById(R.id.thumbnail_preview);

        requestThumbnailPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        startThumbnailCapture();
                    } else {
                        Toast.makeText(requireContext(), R.string.camera_permission_needed, Toast.LENGTH_LONG).show();
                    }
                }
        );

        if (SupabaseRepository.isConfigured()) {
            repository = new SupabaseRepository(requireContext());
        }

        Bundle args = getArguments();
        if (args != null) {
            pendingItemId = args.getString(ARG_ITEM_ID);
            String prefillTitle = args.getString(ARG_PREFILL_TITLE);
            if (prefillTitle != null && !prefillTitle.trim().isEmpty()) {
                ((TextInputEditText) view.findViewById(R.id.input_item_title)).setText(prefillTitle.trim());
            }

            String videoPath = args.getString(ARG_VIDEO_PATH);
            if (videoPath != null && !videoPath.trim().isEmpty()) {
                File videoFile = new File(videoPath);
                selectedVideoUri = Uri.fromFile(videoFile);
                videoFileNameLabel.setText(videoFile.getName());
                videoFileNameLabel.setVisibility(View.VISIBLE);
                videoPlaceholderLabel.setText(getString(R.string.sell_video_selected));
            }
        }

        // Pick video -> opens camera capture or file picker
        view.findViewById(R.id.btn_pick_video).setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Add Video")
                    .setItems(new CharSequence[]{
                            getString(R.string.sell_record_video),
                            getString(R.string.sell_pick_video)
                    }, (dialog, which) -> {
                        if (which == 0) {
                            startActivity(new Intent(requireContext(), CameraCaptureActivity.class));
                        } else {
                            pickVideoLauncher.launch("video/*");
                        }
                    })
                    .show();
        });

        view.findViewById(R.id.btn_pick_thumbnail).setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                startThumbnailCapture();
            } else {
                requestThumbnailPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        // Category dropdown
        String[] categories = { "Furniture", "Electronics", "Sports", "Clothing", "Art", "Decor", "Other" };
        AutoCompleteTextView categoryInput = view.findViewById(R.id.input_item_category);
        categoryInput.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, categories));

        // Cancel
        ((MaterialButton) view.findViewById(R.id.btn_cancel)).setOnClickListener(v -> clearForm(view));

        // List Item
        ((MaterialButton) view.findViewById(R.id.btn_list_item)).setOnClickListener(v -> submitListing(view));
    }

    @Override
    public void onDestroy() {
        if (repository != null) {
            repository.shutdown();
        }
        super.onDestroy();
    }

    private void submitListing(View root) {
        TextInputLayout titleLayout = root.findViewById(R.id.layout_item_title);
        TextInputLayout priceLayout = root.findViewById(R.id.layout_item_price);
        TextInputLayout categoryLayout = root.findViewById(R.id.layout_item_category);
        TextInputLayout locationLayout = root.findViewById(R.id.layout_item_location);

        TextInputEditText titleInput = root.findViewById(R.id.input_item_title);
        TextInputEditText priceInput = root.findViewById(R.id.input_item_price);
        AutoCompleteTextView categoryInput = root.findViewById(R.id.input_item_category);
        TextInputEditText locationInput = root.findViewById(R.id.input_item_location);
        TextInputEditText sellerInput = root.findViewById(R.id.input_seller_name);
        TextInputEditText descriptionInput = root.findViewById(R.id.input_item_description);

        String title = Objects.toString(titleInput.getText(), "").trim();
        String priceText = Objects.toString(priceInput.getText(), "").trim();
        String category = categoryInput.getText().toString().trim();
        String location = Objects.toString(locationInput.getText(), "").trim();
        String sellerName = Objects.toString(sellerInput.getText(), "").trim();
        String description = Objects.toString(descriptionInput.getText(), "").trim();

        boolean valid = true;

        if (selectedVideoUri == null) {
            videoPlaceholderLabel.setText(getString(R.string.sell_video_required));
            valid = false;
        }
        if (selectedThumbnailFile == null) {
            thumbnailPlaceholderLabel.setText(getString(R.string.sell_thumbnail_required));
            valid = false;
        }
        if (title.isEmpty()) { titleLayout.setError(getString(R.string.sell_error_required)); valid = false; }
        else titleLayout.setError(null);
        if (category.isEmpty()) { categoryLayout.setError(getString(R.string.sell_error_required)); valid = false; }
        else categoryLayout.setError(null);
        if (location.isEmpty()) { locationLayout.setError(getString(R.string.sell_error_required)); valid = false; }
        else locationLayout.setError(null);

        Double price = null;
        if (!priceText.isEmpty()) {
            try {
                price = Double.parseDouble(priceText);
                priceLayout.setError(null);
            } catch (NumberFormatException ex) {
                priceLayout.setError(getString(R.string.sell_error_invalid_price));
                valid = false;
            }
        } else {
            priceLayout.setError(null);
        }

        if (!valid) return;

        if (repository == null || !SupabaseRepository.isConfigured()) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.sell_error_title))
                    .setMessage(getString(R.string.missing_supabase_config))
                    .setPositiveButton(getString(R.string.sell_success_ok), null)
                    .show();
            return;
        }

        String videoPath = resolveVideoPath(selectedVideoUri);
        if (videoPath == null) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.sell_error_title))
                    .setMessage(getString(R.string.video_file_missing))
                    .setPositiveButton(getString(R.string.sell_success_ok), null)
                    .show();
            return;
        }

        if (pendingItemId != null && !pendingItemId.trim().isEmpty()) {
            repository.uploadThumbnailAndUpdateListingAsync(
                    pendingItemId,
                    selectedThumbnailFile.getAbsolutePath(),
                    title,
                    sellerName,
                    location,
                    category,
                    description,
                    price,
                    new SupabaseRepository.RepositoryCallback<Void>() {
                        @Override
                        public void onSuccess(@Nullable Void data) {
                            new MaterialAlertDialogBuilder(requireContext())
                                    .setTitle(getString(R.string.sell_success_title))
                                    .setMessage(getString(R.string.sell_success_message, title))
                                    .setPositiveButton(getString(R.string.sell_success_ok), (dialog, which) -> clearForm(root))
                                    .show();
                        }

                        @Override
                        public void onError(@NonNull String message, @Nullable Throwable throwable) {
                            new MaterialAlertDialogBuilder(requireContext())
                                    .setTitle(getString(R.string.sell_error_title))
                                    .setMessage(getString(R.string.sell_error_message, message))
                                    .setPositiveButton(getString(R.string.sell_success_ok), null)
                                    .show();
                        }
                    }
            );
            return;
        }

        repository.createAndStartGenerationWithThumbnail(
                title,
                videoPath,
                selectedThumbnailFile.getAbsolutePath(),
                sellerName,
                location,
                category,
                description,
                price,
                new SupabaseRepository.RepositoryCallback<be.kuleuven.gt.extendedrealityproject.supabase.MarketplaceItemRecord>() {
                    @Override
                    public void onSuccess(@Nullable be.kuleuven.gt.extendedrealityproject.supabase.MarketplaceItemRecord data) {
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle(getString(R.string.sell_success_title))
                                .setMessage(getString(R.string.sell_success_message, title))
                                .setPositiveButton(getString(R.string.sell_success_ok), (dialog, which) -> clearForm(root))
                                .show();
                    }

                    @Override
                    public void onError(@NonNull String message, @Nullable Throwable throwable) {
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle(getString(R.string.sell_error_title))
                                .setMessage(getString(R.string.sell_error_message, message))
                                .setPositiveButton(getString(R.string.sell_success_ok), null)
                                .show();
                    }
                }
        );
    }

    private void startThumbnailCapture() {
        File cacheDir = new File(requireContext().getCacheDir(), "thumbnails");
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            Toast.makeText(requireContext(), R.string.sell_thumbnail_capture_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        selectedThumbnailFile = new File(cacheDir, "thumb_" + System.currentTimeMillis() + ".jpg");
        selectedThumbnailUri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".fileprovider",
                selectedThumbnailFile
        );
        takeThumbnailLauncher.launch(selectedThumbnailUri);
    }

    @Nullable
    private String resolveVideoPath(@Nullable Uri uri) {
        if (uri == null) {
            return null;
        }
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        ContentResolver resolver = requireContext().getContentResolver();
        File cacheDir = new File(requireContext().getCacheDir(), "uploads");
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            return null;
        }

        File outputFile = new File(cacheDir, "video_" + System.currentTimeMillis() + ".mp4");
        try (java.io.InputStream input = resolver.openInputStream(uri);
             java.io.FileOutputStream output = new java.io.FileOutputStream(outputFile)) {
            if (input == null) {
                return null;
            }
            byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            output.flush();
            return outputFile.getAbsolutePath();
        } catch (Exception ignored) {
            return null;
        }
    }

    private void clearForm(View root) {
        ((TextInputEditText) root.findViewById(R.id.input_item_title)).setText("");
        ((TextInputEditText) root.findViewById(R.id.input_item_price)).setText("");
        ((AutoCompleteTextView) root.findViewById(R.id.input_item_category)).setText("");
        ((TextInputEditText) root.findViewById(R.id.input_item_description)).setText("");
        ((TextInputEditText) root.findViewById(R.id.input_item_location)).setText("");
        ((TextInputEditText) root.findViewById(R.id.input_seller_name)).setText("");
        selectedVideoUri = null;
        selectedThumbnailUri = null;
        selectedThumbnailFile = null;
        videoFileNameLabel.setVisibility(View.GONE);
        videoPlaceholderLabel.setText(R.string.sell_record_video);
        thumbnailFileNameLabel.setVisibility(View.GONE);
        thumbnailPlaceholderLabel.setText(R.string.sell_take_thumbnail);
        thumbnailPreview.setVisibility(View.GONE);
        thumbnailPlaceholder.setVisibility(View.VISIBLE);
    }

    public static SellFragment newInstance(@Nullable String itemId, @Nullable String title, @Nullable String videoPath) {
        SellFragment fragment = new SellFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ITEM_ID, itemId);
        args.putString(ARG_PREFILL_TITLE, title);
        args.putString(ARG_VIDEO_PATH, videoPath);
        fragment.setArguments(args);
        return fragment;
    }
}
