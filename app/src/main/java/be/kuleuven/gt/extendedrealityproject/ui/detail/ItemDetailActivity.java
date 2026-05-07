package be.kuleuven.gt.extendedrealityproject.ui.detail;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewAssetLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.Locale;

import be.kuleuven.gt.extendedrealityproject.R;
import be.kuleuven.gt.extendedrealityproject.ar.ArViewerContract;
import be.kuleuven.gt.extendedrealityproject.camera.ModelViewerActivity;
import be.kuleuven.gt.extendedrealityproject.ui.DummyData;
import be.kuleuven.gt.extendedrealityproject.ui.MarketplaceItem;
import be.kuleuven.gt.extendedrealityproject.ui.ImageLoader;
import be.kuleuven.gt.extendedrealityproject.supabase.SupabaseRepository;

import com.google.android.material.chip.Chip;
import com.google.android.material.tabs.TabLayout;

public class ItemDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ITEM_ID = "extra_item_id";
    public static final String EXTRA_ITEM_TITLE = "extra_item_title";
    public static final String EXTRA_ITEM_PRICE = "extra_item_price";
    public static final String EXTRA_ITEM_DESCRIPTION = "extra_item_description";
    public static final String EXTRA_ITEM_SELLER = "extra_item_seller";
    public static final String EXTRA_ITEM_LOCATION = "extra_item_location";
    public static final String EXTRA_ITEM_CATEGORY = "extra_item_category";
    public static final String EXTRA_ITEM_MODEL_URL = "extra_item_model_url";
    public static final String EXTRA_ITEM_THUMBNAIL_URL = "extra_item_thumbnail_url";

    private static final String QUICK_AR_ASSET_PATH = "models/bottle.glb";
    private static final String MODEL_ASSET_BASE = "https://appassets.androidplatform.net";
    private static final String TAG = "ItemDetailActivity";

    @Nullable
    private SupabaseRepository repository;
    @Nullable
    private WebViewAssetLoader assetLoader;
    private WebView modelWebView;
    private View modelContainer;
    private View detailImage;
    private View detailScroll;
    private View modelLoadingIndicator;
    private boolean modelLoaded = false;
    private boolean modelLoading = false;
    @Nullable
    private String modelItemId;
    @Nullable
    private String modelUrl;
    @Nullable
    private File cachedModelFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        // Back button
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());
        findViewById(R.id.btn_back_label).setOnClickListener(v -> finish());
        findViewById(R.id.back_row).setOnClickListener(v -> finish());

        String itemId = getIntent().getStringExtra(EXTRA_ITEM_ID);
        MarketplaceItem item = itemFromIntent(itemId);
        if (item == null) {
            item = findItem(itemId);
        }

        if (item == null) {
            finish();
            return;
        }
        final MarketplaceItem resolvedItem = item;
        modelItemId = resolvedItem.getItemId();
        modelUrl = resolvedItem.getModelUrl();

        modelContainer = findViewById(R.id.detail_model_container);
        modelWebView = findViewById(R.id.detail_model_webview);
        detailImage = findViewById(R.id.detail_image);
        detailScroll = findViewById(R.id.detail_scroll_content);
        modelLoadingIndicator = findViewById(R.id.detail_model_loading);
        setupWebView(modelWebView);

        // Header image
        ImageView headerImage = (ImageView) detailImage;
        if (resolvedItem.getThumbnailUrl() != null && !resolvedItem.getThumbnailUrl().trim().isEmpty()) {
            int targetWidthPx = resolveDetailImageWidthPx(headerImage);
            int targetHeightPx = dpToPx(headerImage, 240);
            String detailUrl = buildDetailThumbnailUrl(resolvedItem.getThumbnailUrl(), targetWidthPx);
            ImageLoader.loadIntoSized(headerImage, detailUrl, R.drawable.placeholder_item,
                    targetWidthPx, targetHeightPx);
        } else if (resolvedItem.getImageResIds() != null && !resolvedItem.getImageResIds().isEmpty()) {
            headerImage.setImageResource(resolvedItem.getImageResIds().get(0));
        }

        // Fields
        ((TextView) findViewById(R.id.detail_title)).setText(resolvedItem.getTitle());

        Double priceValue = resolvedItem.getPrice();
        if (priceValue == null) {
            ((TextView) findViewById(R.id.detail_price)).setText("N/A");
        } else {
            double p = priceValue;
            String priceStr = (p == Math.floor(p))
                    ? String.format(Locale.getDefault(), "€%d", (int) p)
                    : String.format(Locale.getDefault(), "€%.2f", p);
            ((TextView) findViewById(R.id.detail_price)).setText(priceStr);
        }

        ((TextView) findViewById(R.id.detail_description)).setText(resolvedItem.getDescription());
        ((TextView) findViewById(R.id.detail_location))
                .setText(getString(R.string.detail_location_value, resolvedItem.getLocation()));
        ((Chip) findViewById(R.id.detail_category_chip)).setText(resolvedItem.getCategory());

        // Seller
        String sellerName = resolvedItem.getSellerName();
        ((TextView) findViewById(R.id.detail_seller_name)).setText(sellerName);
        // Avatar initial
        String initial = sellerName.isEmpty() ? "?" : String.valueOf(sellerName.charAt(0)).toUpperCase(Locale.getDefault());
        ((TextView) findViewById(R.id.detail_seller_avatar)).setText(initial);

        // Tabs — Photos / 3D Model / AR View
        TabLayout tabLayout = findViewById(R.id.detail_tab_layout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                if (pos == 1) { // 3D Model
                    showModelTab();
                } else {
                    hideModelTab();
                    if (pos == 2) { // AR View
                        openArViewer(resolvedItem);
                    }
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {
                if (tab.getPosition() == 1) {
                    showModelTab();
                } else if (tab.getPosition() == 2) {
                    openArViewer(resolvedItem);
                }
            }
        });

        // Buy Now
        findViewById(R.id.btn_buy_now).setOnClickListener(v ->
                Toast.makeText(this,
                        getString(R.string.detail_buy_stub_message, resolvedItem.getTitle()),
                        Toast.LENGTH_LONG).show()
        );


        // Contact Seller
        findViewById(R.id.btn_contact_seller).setOnClickListener(v ->
                Toast.makeText(this, "Contact feature coming soon!", Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    protected void onDestroy() {
        if (repository != null) {
            repository.shutdown();
        }
        if (modelWebView != null) {
            modelWebView.destroy();
        }
        super.onDestroy();
    }

    private void showModelTab() {
        modelContainer.setVisibility(View.VISIBLE);
        detailImage.setVisibility(View.GONE);
        detailScroll.setVisibility(View.GONE);
        if (!modelLoaded && !modelLoading) {
            loadModelIntoWebView();
        }
    }

    private void hideModelTab() {
        modelContainer.setVisibility(View.GONE);
        detailImage.setVisibility(View.VISIBLE);
        detailScroll.setVisibility(View.VISIBLE);
    }

    private void openArViewer(@NonNull MarketplaceItem item) {
        File arModelFile = resolveArModelFile();
        if (arModelFile == null) {
            Toast.makeText(this, getString(R.string.detail_ar_not_available), Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = ArViewerContract.createIntent(
                this,
                Uri.fromFile(arModelFile),
                arModelFile.getAbsolutePath(),
                item.getTitle()
        );
        startActivity(intent);
    }

    private void loadModelIntoWebView() {
        if (modelUrl == null || modelUrl.trim().isEmpty() || modelItemId == null || modelItemId.trim().isEmpty()) {
            Log.w(TAG, "3D model missing modelUrl/itemId. modelUrl=" + modelUrl + " itemId=" + modelItemId);
            Toast.makeText(this, getString(R.string.detail_ar_not_available), Toast.LENGTH_SHORT).show();
            return;
        }
        if (!SupabaseRepository.isConfigured()) {
            Log.w(TAG, "Supabase not configured; cannot fetch model.");
            Toast.makeText(this, getString(R.string.missing_supabase_config), Toast.LENGTH_SHORT).show();
            return;
        }

        File cachedFile = getLocalPlyFile(modelItemId);
        if (cachedFile.exists() && cachedFile.isFile() && cachedFile.length() > 0) {
            Log.d(TAG, "Using cached model: " + cachedFile.getAbsolutePath() + " size=" + cachedFile.length());
            cachedModelFile = cachedFile;
            loadCachedModel(cachedFile);
            return;
        }

        if (repository == null) {
            repository = new SupabaseRepository(this);
        }

        modelLoading = true;
        modelLoadingIndicator.setVisibility(View.VISIBLE);
        Log.d(TAG, "Downloading model: itemId=" + modelItemId + " url=" + modelUrl);
        repository.downloadAndExtractPlyAsync(modelItemId, modelUrl, new SupabaseRepository.RepositoryCallback<>() {
            @Override
            public void onSuccess(@Nullable File data) {
                modelLoading = false;
                if (data == null || !data.exists()) {
                    Log.e(TAG, "Downloaded model missing or null. file=" + data);
                    modelLoadingIndicator.setVisibility(View.GONE);
                    Toast.makeText(ItemDetailActivity.this, R.string.model_extract_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.d(TAG, "Downloaded model ready: " + data.getAbsolutePath() + " size=" + data.length());
                cachedModelFile = data;
                loadCachedModel(data);
            }

            @Override
            public void onError(@NonNull String message, @Nullable Throwable throwable) {
                modelLoading = false;
                modelLoadingIndicator.setVisibility(View.GONE);
                Log.e(TAG, "Download failed: " + message, throwable);
                Toast.makeText(ItemDetailActivity.this, getString(R.string.marketplace_download_failed, message), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadCachedModel(@NonNull File modelFile) {
        assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(ItemDetailActivity.this))
                .addPathHandler("/cache/", new CachePathHandler(getCacheDir()))
                .build();

        String modelRelativePath = toRelativeCachePath(modelFile);
        String modelFileUrl = MODEL_ASSET_BASE + "/cache/" + modelRelativePath;
        String viewerUrl = MODEL_ASSET_BASE + "/assets/www/model_viewer.html?model="
                + android.net.Uri.encode(modelFileUrl);

        Log.d(TAG, "Loading WebView: model=" + modelFileUrl + " viewer=" + viewerUrl);
        modelWebView.loadUrl(viewerUrl);
        modelLoaded = true;
    }

    @NonNull
    private File getLocalPlyFile(@NonNull String itemId) {
        return new File(getCacheDir(), "models/" + itemId + "/model.ply");
    }

    private void setupWebView(@NonNull WebView webView) {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader == null ? null : assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "WebView loaded: " + url);
                modelLoadingIndicator.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    String msg = getString(R.string.viewer_web_error, String.valueOf(error.getDescription()));
                    Log.e(TAG, "WebView error: " + msg);
                    modelLoadingIndicator.setVisibility(View.GONE);
                    Toast.makeText(ItemDetailActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void open3DModel(MarketplaceItem item) {
        if (cachedModelFile != null && cachedModelFile.exists()) {
            Intent intent = new Intent(this, ModelViewerActivity.class);
            intent.putExtra(ModelViewerActivity.EXTRA_MODEL_PATH, cachedModelFile.getAbsolutePath());
            intent.putExtra(ModelViewerActivity.EXTRA_MODEL_TITLE, item.getTitle());
            startActivity(intent);
            return;
        }

        File cachedFile = modelItemId == null ? null : getLocalPlyFile(modelItemId);
        if (cachedFile != null && cachedFile.exists() && cachedFile.isFile() && cachedFile.length() > 0) {
            cachedModelFile = cachedFile;
            Intent intent = new Intent(this, ModelViewerActivity.class);
            intent.putExtra(ModelViewerActivity.EXTRA_MODEL_PATH, cachedFile.getAbsolutePath());
            intent.putExtra(ModelViewerActivity.EXTRA_MODEL_TITLE, item.getTitle());
            startActivity(intent);
            return;
        }

        if (modelUrl == null || modelUrl.trim().isEmpty() || modelItemId == null || modelItemId.trim().isEmpty()) {
            Toast.makeText(this, getString(R.string.detail_ar_not_available), Toast.LENGTH_SHORT).show();
            return;
        }
        if (!SupabaseRepository.isConfigured()) {
            Toast.makeText(this, getString(R.string.missing_supabase_config), Toast.LENGTH_SHORT).show();
            return;
        }
        if (repository == null) {
            repository = new SupabaseRepository(this);
        }

        Log.d(TAG, "AR download requested: itemId=" + modelItemId + " url=" + modelUrl);
        repository.downloadAndExtractPlyAsync(modelItemId, modelUrl, new SupabaseRepository.RepositoryCallback<>() {
            @Override
            public void onSuccess(@Nullable File data) {
                if (data == null || !data.exists()) {
                    Log.e(TAG, "AR download missing file. file=" + data);
                    Toast.makeText(ItemDetailActivity.this, R.string.model_extract_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
                cachedModelFile = data;
                Intent intent = new Intent(ItemDetailActivity.this, ModelViewerActivity.class);
                intent.putExtra(ModelViewerActivity.EXTRA_MODEL_PATH, data.getAbsolutePath());
                intent.putExtra(ModelViewerActivity.EXTRA_MODEL_TITLE, item.getTitle());
                startActivity(intent);
            }

            @Override
            public void onError(@NonNull String message, @Nullable Throwable throwable) {
                Log.e(TAG, "AR download failed: " + message, throwable);
                Toast.makeText(ItemDetailActivity.this, getString(R.string.marketplace_download_failed, message), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Nullable
    private File resolveArModelFile() {
        if (cachedModelFile != null && cachedModelFile.exists() && isArRenderable(cachedModelFile)) {
            return cachedModelFile;
        }

        if (modelItemId != null) {
            File itemDir = new File(getCacheDir(), "models/" + modelItemId);
            File glb = new File(itemDir, "model.glb");
            if (glb.exists() && glb.isFile() && glb.length() > 0) {
                return glb;
            }
            File gltf = new File(itemDir, "model.gltf");
            if (gltf.exists() && gltf.isFile() && gltf.length() > 0) {
                return gltf;
            }
        }

        try {
            return ensureBundledDemoInCache();
        } catch (IOException exception) {
            Log.w(TAG, "Bundled AR demo asset missing.", exception);
            return null;
        }
    }

    private boolean isArRenderable(@NonNull File file) {
        String name = file.getName().toLowerCase(Locale.US);
        return name.endsWith(".glb") || name.endsWith(".gltf");
    }

    private File ensureBundledDemoInCache() throws IOException {
        File cacheDir = new File(getCacheDir(), "ar-demo");
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            throw new IOException("Could not create AR demo cache directory");
        }

        File outFile = new File(cacheDir, "bottle.glb");
        if (outFile.exists() && outFile.length() > 0) {
            return outFile;
        }

        try (InputStream in = getAssets().open(QUICK_AR_ASSET_PATH);
             FileOutputStream out = new FileOutputStream(outFile, false)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.getFD().sync();
        }

        if (!outFile.exists() || outFile.length() == 0) {
            throw new IOException("AR demo file is empty");
        }
        return outFile;
    }

    private MarketplaceItem findItem(String id) {
        if (id == null) return null;
        for (MarketplaceItem item : DummyData.getItems()) {
            if (item.getItemId().equals(id)) return item;
        }
        return null;
    }

    @Nullable
    private MarketplaceItem itemFromIntent(@Nullable String itemId) {
        Intent intent = getIntent();
        if (intent == null) {
            return null;
        }

        String title = trimToNull(intent.getStringExtra(EXTRA_ITEM_TITLE));
        if (itemId == null || title == null) {
            return null;
        }

        Double price = null;
        if (intent.hasExtra(EXTRA_ITEM_PRICE)) {
            price = intent.getDoubleExtra(EXTRA_ITEM_PRICE, 0.0);
        }

        return new MarketplaceItem(
                itemId,
                title,
                price,
                fallback(intent.getStringExtra(EXTRA_ITEM_DESCRIPTION), "No description available."),
                fallback(intent.getStringExtra(EXTRA_ITEM_SELLER), "Marketplace Seller"),
                fallback(intent.getStringExtra(EXTRA_ITEM_LOCATION), "Unknown location"),
                fallback(intent.getStringExtra(EXTRA_ITEM_CATEGORY), "Other"),
                trimToNull(intent.getStringExtra(EXTRA_ITEM_MODEL_URL)),
                trimToNull(intent.getStringExtra(EXTRA_ITEM_THUMBNAIL_URL)),
                null
        );
    }

    @Nullable
    private String trimToNull(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @NonNull
    private String fallback(@Nullable String value, @NonNull String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed == null ? defaultValue : trimmed;
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

    @NonNull
    private String buildDetailThumbnailUrl(@NonNull String thumbnailUrl, int targetWidthPx) {
        String trimmed = thumbnailUrl.trim();
        int requestedWidth = Math.min(900, Math.max(1, targetWidthPx));
        String withWidth = trimmed;
        if (withWidth.contains("width=")) {
            withWidth = withWidth.replaceAll("width=\\d+", "width=" + requestedWidth);
        } else if (withWidth.contains("?")) {
            withWidth = withWidth + "&width=" + requestedWidth;
        } else {
            withWidth = withWidth + "?width=" + requestedWidth;
        }
        if (!withWidth.contains("resize=")) {
            withWidth = withWidth + "&resize=contain";
        }
        if (!withWidth.contains("format=")) {
            withWidth = withWidth + "&format=webp";
        }
        return withWidth;
    }

    private int resolveDetailImageWidthPx(@NonNull ImageView image) {
        int screenWidthPx = image.getResources().getDisplayMetrics().widthPixels;
        int horizontalPaddingPx = dpToPx(image, 32);
        int availableWidth = Math.max(1, screenWidthPx - horizontalPaddingPx);
        return availableWidth;
    }

    private int dpToPx(@NonNull ImageView image, int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                image.getResources().getDisplayMetrics()));
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
                String cleaned = URLDecoder.decode(path, "UTF-8");
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

                return new WebResourceResponse(mimeType, encoding, stream);
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
            return null;
        }
    }
}
