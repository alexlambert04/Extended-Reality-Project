package be.kuleuven.gt.extendedrealityproject.ui.sell;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import be.kuleuven.gt.extendedrealityproject.R;
import be.kuleuven.gt.extendedrealityproject.camera.CameraCaptureActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

public class SellFragment extends Fragment {

    private Uri selectedVideoUri = null;
    private int photoCount = 0;
    private boolean isPhotoMode = true;

    private LinearLayout photosSection;
    private LinearLayout videoSection;
    private TextView photosCountLabel;
    private TextView videoFileNameLabel;
    private TextView videoPlaceholderLabel;

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

    private final ActivityResultLauncher<String> pickPhotoLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    photoCount++;
                    photosCountLabel.setText(getString(R.string.sell_photos_count, photoCount));
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

        photosSection = view.findViewById(R.id.photos_section);
        videoSection = view.findViewById(R.id.video_section);
        photosCountLabel = view.findViewById(R.id.photos_count_label);
        videoFileNameLabel = view.findViewById(R.id.video_file_name_label);
        videoPlaceholderLabel = view.findViewById(R.id.video_placeholder_label);

        // Method toggle
        LinearLayout methodPhotos = view.findViewById(R.id.method_photos_card);
        LinearLayout methodVideo = view.findViewById(R.id.method_video_card);

        methodPhotos.setOnClickListener(v -> selectMode(true, methodPhotos, methodVideo));
        methodVideo.setOnClickListener(v -> selectMode(false, methodPhotos, methodVideo));

        // Add photo
        view.findViewById(R.id.btn_add_photo).setOnClickListener(v ->
                pickPhotoLauncher.launch("image/*"));

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

    private void selectMode(boolean photoMode, LinearLayout photosCard, LinearLayout videoCard) {
        isPhotoMode = photoMode;
        photosCard.setBackgroundResource(photoMode ? R.drawable.bg_capture_selected : R.drawable.bg_capture_unselected);
        videoCard.setBackgroundResource(photoMode ? R.drawable.bg_capture_unselected : R.drawable.bg_capture_selected);
        photosSection.setVisibility(photoMode ? View.VISIBLE : View.GONE);
        videoSection.setVisibility(photoMode ? View.GONE : View.VISIBLE);
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

        String title = Objects.toString(titleInput.getText(), "").trim();
        String price = Objects.toString(priceInput.getText(), "").trim();
        String category = categoryInput.getText().toString().trim();
        String location = Objects.toString(locationInput.getText(), "").trim();

        boolean valid = true;

        boolean hasMedia = (isPhotoMode && photoCount > 0) || (!isPhotoMode && selectedVideoUri != null);
        if (!hasMedia) {
            photosCountLabel.setText(getString(R.string.sell_video_required));
            valid = false;
        }
        if (title.isEmpty()) { titleLayout.setError(getString(R.string.sell_error_required)); valid = false; }
        else titleLayout.setError(null);
        if (price.isEmpty()) { priceLayout.setError(getString(R.string.sell_error_required)); valid = false; }
        else priceLayout.setError(null);
        if (category.isEmpty()) { categoryLayout.setError(getString(R.string.sell_error_required)); valid = false; }
        else categoryLayout.setError(null);
        if (location.isEmpty()) { locationLayout.setError(getString(R.string.sell_error_required)); valid = false; }
        else locationLayout.setError(null);

        if (!valid) return;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.sell_success_title))
                .setMessage(getString(R.string.sell_success_message, title))
                .setPositiveButton(getString(R.string.sell_success_ok), (dialog, which) -> clearForm(root))
                .show();
    }

    private void clearForm(View root) {
        ((TextInputEditText) root.findViewById(R.id.input_item_title)).setText("");
        ((TextInputEditText) root.findViewById(R.id.input_item_price)).setText("");
        ((AutoCompleteTextView) root.findViewById(R.id.input_item_category)).setText("");
        ((TextInputEditText) root.findViewById(R.id.input_item_description)).setText("");
        ((TextInputEditText) root.findViewById(R.id.input_item_location)).setText("");
        ((TextInputEditText) root.findViewById(R.id.input_seller_name)).setText("");
        selectedVideoUri = null;
        photoCount = 0;
        videoFileNameLabel.setVisibility(View.GONE);
        videoPlaceholderLabel.setText(R.string.sell_record_video);
        photosCountLabel.setText(getString(R.string.sell_photos_count, 0));
    }
}
