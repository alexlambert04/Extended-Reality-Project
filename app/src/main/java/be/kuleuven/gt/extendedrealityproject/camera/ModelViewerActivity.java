package be.kuleuven.gt.extendedrealityproject.camera;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.MimeTypeMap;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewAssetLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import be.kuleuven.gt.extendedrealityproject.R;
import be.kuleuven.gt.extendedrealityproject.databinding.ActivityModelViewerBinding;

public class ModelViewerActivity extends AppCompatActivity {

    public static final String EXTRA_MODEL_PATH = "extra_model_path";
    public static final String EXTRA_MODEL_TITLE = "extra_model_title";

    private static final String TAG = "ModelViewerActivity";

    private ActivityModelViewerBinding binding;
    private WebViewAssetLoader assetLoader;
    private boolean isDebugExpanded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityModelViewerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String modelPath = getIntent().getStringExtra(EXTRA_MODEL_PATH);
        String title = getIntent().getStringExtra(EXTRA_MODEL_TITLE);

        if (title != null && !title.trim().isEmpty()) {
            binding.viewerTitle.setText(getString(R.string.viewer_title_with_name, title));
        }

        if (modelPath == null || modelPath.trim().isEmpty()) {
            Toast.makeText(this, R.string.viewer_missing_model, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        File modelFile = new File(modelPath);
        if (!modelFile.exists() || !modelFile.isFile()) {
            Toast.makeText(this, R.string.viewer_missing_model, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Use custom cache handler to avoid InternalStoragePathHandler restrictions on cache dir.
        assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .addPathHandler("/cache/", new CachePathHandler(getCacheDir()))
                .build();

        String modelRelativePath = toRelativeCachePath(modelFile);
        String modelFileUrl = "https://appassets.androidplatform.net/cache/" + modelRelativePath;
        String viewerUrl = "https://appassets.androidplatform.net/assets/www/model_viewer.html?model="
                + android.net.Uri.encode(modelFileUrl);

        Log.d(TAG, "Model absolute path: " + modelPath);
        Log.d(TAG, "Model relative path: " + modelRelativePath);
        Log.d(TAG, "Model URL for WebView: " + modelFileUrl);
        Log.d(TAG, "Viewer URL: " + viewerUrl);

        String preflight = String.format(
                Locale.US,
                "Local model file: %s\nFile size: %.2f MB\nViewer URL: %s",
                modelPath,
                modelFile.length() / (1024.0 * 1024.0),
                viewerUrl
        );
        binding.viewerDebugText.setText(preflight);
        binding.viewerDebugText.setMovementMethod(new ScrollingMovementMethod());
        binding.debugToggleRow.setOnClickListener(view -> toggleDebugPanel());
        updateDebugPanelState();

        setupWebView(binding.modelWebView);
        binding.modelWebView.loadUrl(viewerUrl);
    }

    @Override
    protected void onDestroy() {
        if (binding != null) {
            binding.modelWebView.destroy();
        }
        super.onDestroy();
    }

    private void setupWebView(@NonNull WebView webView) {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);

        WebView.setWebContentsDebuggingEnabled(true);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(@NonNull ConsoleMessage consoleMessage) {
                String msg = "JS " + consoleMessage.messageLevel() + ": " + consoleMessage.message();
                Log.d(TAG, msg);
                appendDebug(msg);
                return true;
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                return assetLoader.shouldInterceptRequest(android.net.Uri.parse(url));
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "Page loaded: " + url);
                appendDebug("Page loaded: " + url);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    String msg = getString(R.string.viewer_web_error, String.valueOf(error.getDescription()));
                    Log.e(TAG, msg);
                    appendDebug(msg);
                }
            }
        });
    }

    private void appendDebug(@NonNull String line) {
        CharSequence current = binding.viewerDebugText.getText();
        binding.viewerDebugText.setText((current + "\n" + line).trim());
    }

    private void toggleDebugPanel() {
        isDebugExpanded = !isDebugExpanded;
        updateDebugPanelState();
    }

    private void updateDebugPanelState() {
        binding.viewerDebugText.setVisibility(isDebugExpanded ? android.view.View.VISIBLE : android.view.View.GONE);
        binding.debugToggleLabel.setText(isDebugExpanded ? R.string.viewer_debug_hide : R.string.viewer_debug_show);
    }

    @NonNull
    private String toRelativeCachePath(@NonNull File modelFile) {
        String cachePath = getCacheDir().getAbsolutePath();
        String modelPath = modelFile.getAbsolutePath();
        if (modelPath.startsWith(cachePath)) {
            String relative = modelPath.substring(cachePath.length());
            while (relative.startsWith(File.separator)) {
                relative = relative.substring(1);
            }
            return relative.replace('\\', '/');
        }
        return modelFile.getName();
    }

    private static final class CachePathHandler implements WebViewAssetLoader.PathHandler {
        private final File cacheRoot;

        private CachePathHandler(@NonNull File cacheRoot) {
            this.cacheRoot = cacheRoot;
        }

        @Nullable
        @Override
        public WebResourceResponse handle(@NonNull String path) {
            try {
                String cleaned = URLDecoder.decode(path, StandardCharsets.UTF_8.name());
                while (cleaned.startsWith("/")) {
                    cleaned = cleaned.substring(1);
                }

                File file = new File(cacheRoot, cleaned);
                String rootCanonical = cacheRoot.getCanonicalPath();
                String fileCanonical = file.getCanonicalPath();
                if (!fileCanonical.startsWith(rootCanonical) || !file.exists() || !file.isFile()) {
                    return null;
                }

                InputStream stream = new FileInputStream(file);
                String mimeType = resolveMimeType(file.getName());
                String encoding = resolveEncoding(mimeType);

                Log.d(TAG, "Serving cache file: " + fileCanonical + " | mime=" + mimeType + " | encoding=" + encoding + " | size=" + file.length());
                return new WebResourceResponse(mimeType, encoding, stream);
            } catch (FileNotFoundException fileNotFoundException) {
                return null;
            } catch (Exception ignored) {
                return null;
            }
        }

        @NonNull
        private String resolveMimeType(@NonNull String fileName) {
            String extension = MimeTypeMap.getFileExtensionFromUrl(fileName);
            if (extension == null || extension.trim().isEmpty()) {
                return "application/octet-stream";
            }
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase(Locale.US));
            return mime == null ? "application/octet-stream" : mime;
        }

        @Nullable
        private String resolveEncoding(@NonNull String mimeType) {
            if (mimeType.startsWith("text/") || mimeType.contains("json") || mimeType.contains("xml") || mimeType.contains("javascript")) {
                return "utf-8";
            }
            // Important: binary assets (like model.ply/.splat) must not declare UTF-8 encoding.
            return null;
        }
    }
}
