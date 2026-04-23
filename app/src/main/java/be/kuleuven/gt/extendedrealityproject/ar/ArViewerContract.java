package be.kuleuven.gt.extendedrealityproject.ar;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class ArViewerContract {

    public static final String EXTRA_MODEL_URI = "be.kuleuven.gt.extendedrealityproject.ar.extra.MODEL_URI";
    public static final String EXTRA_MODEL_PATH = "be.kuleuven.gt.extendedrealityproject.ar.extra.MODEL_PATH";
    public static final String EXTRA_MODEL_TITLE = "be.kuleuven.gt.extendedrealityproject.ar.extra.MODEL_TITLE";

    static final String ARG_MODEL_URI = "arg_model_uri";
    static final String ARG_MODEL_PATH = "arg_model_path";

    private ArViewerContract() {
    }

    @NonNull
    public static Intent createIntent(
            @NonNull Context context,
            @Nullable Uri modelUri,
            @Nullable String modelPath,
            @Nullable String modelTitle
    ) {
        Intent intent = new Intent(context, ArViewerActivity.class);
        if (modelUri != null) {
            intent.putExtra(EXTRA_MODEL_URI, modelUri.toString());
        }
        if (modelPath != null && !modelPath.trim().isEmpty()) {
            intent.putExtra(EXTRA_MODEL_PATH, modelPath);
        }
        if (modelTitle != null && !modelTitle.trim().isEmpty()) {
            intent.putExtra(EXTRA_MODEL_TITLE, modelTitle);
        }
        return intent;
    }

    @NonNull
    static Bundle toFragmentArgs(@NonNull Intent intent) {
        Bundle args = new Bundle();
        String modelUri = intent.getStringExtra(EXTRA_MODEL_URI);
        if (modelUri != null && !modelUri.trim().isEmpty()) {
            args.putString(ARG_MODEL_URI, modelUri);
        }

        String modelPath = intent.getStringExtra(EXTRA_MODEL_PATH);
        if (modelPath != null && !modelPath.trim().isEmpty()) {
            args.putString(ARG_MODEL_PATH, modelPath);
        }
        return args;
    }

    @Nullable
    static Uri resolveModelUri(@Nullable Bundle args) {
        if (args == null) {
            return null;
        }

        String modelUri = args.getString(ARG_MODEL_URI);
        if (modelUri != null && !modelUri.trim().isEmpty()) {
            return Uri.parse(modelUri);
        }

        String modelPath = args.getString(ARG_MODEL_PATH);
        if (modelPath == null || modelPath.trim().isEmpty()) {
            return null;
        }
        return Uri.fromFile(new java.io.File(modelPath));
    }
}

