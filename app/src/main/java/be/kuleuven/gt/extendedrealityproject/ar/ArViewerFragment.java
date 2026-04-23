package be.kuleuven.gt.extendedrealityproject.ar;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.ar.core.Anchor;
import com.google.ar.core.Session;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.concurrent.CompletableFuture;

import be.kuleuven.gt.extendedrealityproject.R;

public class ArViewerFragment extends Fragment {

    private static final String TAG = "ArViewerFragment";
    private static final int AR_INIT_MAX_ATTEMPTS = 20;
    private static final long AR_INIT_RETRY_DELAY_MS = 120L;

    @Nullable
    private ArFragment arFragment;
    @Nullable
    private CompletableFuture<ModelRenderable> renderableFuture;
    @Nullable
    private ModelRenderable modelRenderable;
    @Nullable
    private Anchor anchor;
    @Nullable
    private AnchorNode anchorNode;
    @Nullable
    private TransformableNode transformableNode;
    @Nullable
    private Scene.OnUpdateListener sceneUpdateListener;
    @Nullable
    private Uri pendingSourceUri;
    @Nullable
    private Runnable arInitRunnable;
    private int arInitAttempt;
    private boolean arSetupComplete;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_ar_viewer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Fragment child = getChildFragmentManager().findFragmentById(R.id.ar_fragment_container);
        if (!(child instanceof ArFragment)) {
            Log.e(TAG, "ArFragment host is missing or not an ArFragment instance.");
            showArUnavailableAndFinish();
            return;
        }

        arFragment = (ArFragment) child;

        Uri sourceUri = ArViewerContract.resolveModelUri(getArguments());
        if (sourceUri == null) {
            Log.e(TAG, "Model source URI is null. Check intent/fragment arguments.");
            Toast.makeText(requireContext(), R.string.viewer_missing_model_source, Toast.LENGTH_SHORT).show();
            requireActivity().finish();
            return;
        }

        pendingSourceUri = sourceUri;
        scheduleArInitialization();
    }

    private void scheduleArInitialization() {
        if (arSetupComplete) {
            return;
        }

        cancelPendingArInitialization();
        arInitAttempt = 0;
        arInitRunnable = this::tryInitializeAr;

        View root = getView();
        if (root != null) {
            root.post(arInitRunnable);
        }
    }

    private void tryInitializeAr() {
        if (!isAdded() || arSetupComplete) {
            return;
        }

        ArSceneView arSceneView = getSafeArSceneView();
        Scene scene = arSceneView != null ? arSceneView.getScene() : null;
        Uri sourceUri = pendingSourceUri;

        if (arFragment == null || arSceneView == null || scene == null || sourceUri == null) {
            if (arInitAttempt < AR_INIT_MAX_ATTEMPTS) {
                arInitAttempt++;
                View root = getView();
                if (root != null && arInitRunnable != null) {
                    root.postDelayed(arInitRunnable, AR_INIT_RETRY_DELAY_MS);
                    return;
                }
            }

            Log.e(TAG, "AR setup unavailable after retries. fragment=" + (arFragment != null)
                    + ", sceneView=" + (arSceneView != null)
                    + ", scene=" + (scene != null)
                    + ", sourceUri=" + (sourceUri != null));
            showArUnavailableAndFinish();
            return;
        }

        initializeArScene(scene, sourceUri);
        arSetupComplete = true;
        cancelPendingArInitialization();
    }

    private void initializeArScene(@NonNull Scene scene, @NonNull Uri sourceUri) {
        final ArFragment localArFragment = arFragment;
        if (localArFragment == null) {
            Log.e(TAG, "ArFragment unexpectedly null during AR scene initialization.");
            showArUnavailableAndFinish();
            return;
        }

        renderableFuture = ModelRenderable.builder()
                .setSource(requireContext(), sourceUri)
                .setIsFilamentGltf(true)
                .build();

        renderableFuture.thenAccept(renderable -> modelRenderable = renderable)
                .exceptionally(throwable -> {
                    Log.e(TAG, "Failed to build ModelRenderable from URI: " + sourceUri, throwable);
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), R.string.viewer_model_load_failed, Toast.LENGTH_SHORT).show();
                            requireActivity().finish();
                        });
                    }
                    return null;
                });

        sceneUpdateListener = frameTime -> {
            if (transformableNode != null && transformableNode.getRenderable() != null && !transformableNode.isSelected()) {
                transformableNode.select();
            }
        };

        scene.addOnUpdateListener(sceneUpdateListener);
        localArFragment.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {
            if (modelRenderable == null) {
                return;
            }

            ArSceneView safeView = getSafeArSceneView();
            if (safeView == null || safeView.getScene() == null) {
                return;
            }

            clearPlacedNode();

            anchor = hitResult.createAnchor();
            anchorNode = new AnchorNode(anchor);
            anchorNode.setParent(safeView.getScene());

            transformableNode = new TransformableNode(localArFragment.getTransformationSystem());
            transformableNode.setParent(anchorNode);
            transformableNode.setRenderable(modelRenderable);
            transformableNode.select();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!arSetupComplete) {
            scheduleArInitialization();
            return;
        }

        ArSceneView arSceneView = getSafeArSceneView();
        if (arSceneView != null) {
            try {
                arSceneView.resume();
            } catch (Exception exception) {
                Log.e(TAG, "AR SceneView failed to resume.", exception);
                showArUnavailableAndFinish();
            }
        }
    }

    @Override
    public void onPause() {
        ArSceneView arSceneView = getSafeArSceneView();
        if (arSceneView != null) {
            arSceneView.pause();
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        ArSceneView arSceneView = getSafeArSceneView();
        if (arSceneView != null) {
            arSceneView.pause();
        }
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        cancelPendingArInitialization();
        detachListeners();
        clearPlacedNode();

        if (renderableFuture != null) {
            renderableFuture.cancel(true);
            renderableFuture = null;
        }
        modelRenderable = null;
        pendingSourceUri = null;
        arSetupComplete = false;

        if (arFragment != null) {
            ArSceneView arSceneView = getSafeArSceneView();
            Session session = arSceneView != null ? arSceneView.getSession() : null;
            if (arSceneView != null) {
                arSceneView.destroy();
            }
            if (session != null) {
                session.close();
            }
            arFragment = null;
        }

        super.onDestroyView();
    }

    private void cancelPendingArInitialization() {
        View root = getView();
        if (root != null && arInitRunnable != null) {
            root.removeCallbacks(arInitRunnable);
        }
        arInitRunnable = null;
    }

    private void detachListeners() {
        if (arFragment == null) {
            return;
        }

        arFragment.setOnTapArPlaneListener(null);
        if (sceneUpdateListener != null) {
            ArSceneView arSceneView = getSafeArSceneView();
            Scene scene = arSceneView != null ? arSceneView.getScene() : null;
            if (scene != null) {
                scene.removeOnUpdateListener(sceneUpdateListener);
            }
            sceneUpdateListener = null;
        }
    }

    @Nullable
    private ArSceneView getSafeArSceneView() {
        return arFragment != null ? arFragment.getArSceneView() : null;
    }

    private void showArUnavailableAndFinish() {
        if (!isAdded()) {
            return;
        }
        Toast.makeText(requireContext(), R.string.viewer_ar_unavailable, Toast.LENGTH_SHORT).show();
        requireActivity().finish();
    }

    private void clearPlacedNode() {
        if (transformableNode != null) {
            transformableNode.setParent(null);
            transformableNode.setRenderable(null);
            transformableNode = null;
        }

        if (anchorNode != null) {
            anchorNode.setParent(null);
            anchorNode = null;
        }

        if (anchor != null) {
            anchor.detach();
            anchor = null;
        }
    }
}



