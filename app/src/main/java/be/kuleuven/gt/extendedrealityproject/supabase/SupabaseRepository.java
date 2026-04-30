package be.kuleuven.gt.extendedrealityproject.supabase;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import be.kuleuven.gt.extendedrealityproject.BuildConfig;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class SupabaseRepository {

    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");
    private static final MediaType VIDEO_MEDIA = MediaType.parse("video/mp4");
    private static final String CREDITS_EXHAUSTED_CODE = "CREDITS_EXHAUSTED";
    private static final String TAG = "SupabaseRepository";

    private final String baseUrl;
    private final String anonKey;
    private final Context appContext;
    private final OkHttpClient httpClient;
    private final ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public SupabaseRepository(@NonNull Context context) {
        this(context, new OkHttpClient());
    }

    public SupabaseRepository(@NonNull Context context, @NonNull OkHttpClient httpClient) {
        this.baseUrl = stripTrailingSlash(BuildConfig.SUPABASE_URL);
        this.anonKey = BuildConfig.SUPABASE_ANON_KEY;
        this.appContext = context.getApplicationContext();
        this.httpClient = httpClient;
        this.executor = Executors.newSingleThreadExecutor();
    }

    public static boolean isConfigured() {
        return BuildConfig.SUPABASE_URL != null
                && !BuildConfig.SUPABASE_URL.trim().isEmpty()
                && BuildConfig.SUPABASE_ANON_KEY != null
                && !BuildConfig.SUPABASE_ANON_KEY.trim().isEmpty();
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    public void createAndStartGeneration(
            @NonNull String title,
            @NonNull String videoPath,
            @NonNull RepositoryCallback<MarketplaceItemRecord> callback
    ) {
        executor.execute(() -> {
            try {
                MarketplaceItemRecord created = insertMarketplaceItem(title);
                String objectPath = created.getId() + "/video.mp4";
                uploadVideo(objectPath, videoPath);
                updateFilePath(created.getId(), objectPath);
                invokeStartKiriJob(created.getId());
                MarketplaceItemRecord current = fetchMarketplaceItem(created.getId());
                postSuccess(callback, current);
            } catch (Exception exception) {
                postError(callback, userMessage(exception), exception);
            }
        });
    }

    public void fetchMarketplaceItemAsync(
            @NonNull String itemId,
            @NonNull RepositoryCallback<MarketplaceItemRecord> callback
    ) {
        executor.execute(() -> {
            try {
                postSuccess(callback, fetchMarketplaceItem(itemId));
            } catch (Exception exception) {
                postError(callback, userMessage(exception), exception);
            }
        });
    }

    public void fetchReadyMarketplaceItemsAsync(
            @NonNull RepositoryCallback<List<MarketplaceItemRecord>> callback
    ) {
        executor.execute(() -> {
            try {
                postSuccess(callback, fetchReadyMarketplaceItems());
            } catch (Exception exception) {
                postError(callback, userMessage(exception), exception);
            }
        });
    }

    public void retryStartKiriJobAsync(
            @NonNull String itemId,
            @NonNull RepositoryCallback<Void> callback
    ) {
        executor.execute(() -> {
            try {
                invokeStartKiriJob(itemId);
                postSuccess(callback, null);
            } catch (Exception exception) {
                postError(callback, userMessage(exception), exception);
            }
        });
    }

    public void fetchAvailableScansAsync(@NonNull RepositoryCallback<Integer> callback) {
        executor.execute(() -> {
            try {
                postSuccess(callback, fetchAvailableScans());
            } catch (Exception exception) {
                postError(callback, userMessage(exception), exception);
            }
        });
    }

    public void downloadAndExtractModelAsync(
            @NonNull String itemId,
            @NonNull String modelUrl,
            @NonNull RepositoryCallback<File> callback
    ) {
        executor.execute(() -> {
            try {
                File plyFile = downloadAndExtractModel(itemId, modelUrl);
                postSuccess(callback, plyFile);
            } catch (Exception exception) {
                postError(callback, userMessage(exception), exception);
            }
        });
    }

    public void downloadAndExtractPlyAsync(
            @NonNull String itemId,
            @NonNull String modelUrl,
            @NonNull RepositoryCallback<File> callback
    ) {
        executor.execute(() -> {
            try {
                File modelFile = downloadAndExtractPly(itemId, modelUrl);
                postSuccess(callback, modelFile);
            } catch (Exception exception) {
                postError(callback, userMessage(exception), exception);
            }
        });
    }

    @NonNull
    public String getRealtimeWebsocketUrl() {
        return baseUrl.replace("https://", "wss://")
                + "/realtime/v1/websocket?apikey=" + anonKey + "&vsn=1.0.0";
    }

    @NonNull
    public String getAnonKey() {
        return anonKey;
    }

    private MarketplaceItemRecord insertMarketplaceItem(@NonNull String title) throws IOException, JSONException {
        JSONObject body = new JSONObject();
        body.put("title", title);
        body.put("status", PipelineStatus.UPLOADING.name());

        Request request = baseApiRequest(baseUrl + "/rest/v1/MarketplaceItems")
                .addHeader("Prefer", "return=representation")
                .post(RequestBody.create(body.toString(), JSON_MEDIA))
                .build();

        JSONObject object = executeForSingleObject(request);
        return parseMarketplaceItem(object);
    }

    private void uploadVideo(@NonNull String objectPath, @NonNull String localVideoPath)
            throws IOException {
        File videoFile = new File(localVideoPath);
        if (!videoFile.exists()) {
            throw new IOException("Video file not found at " + localVideoPath);
        }

        Request request = baseApiRequest(baseUrl + "/storage/v1/object/raw_scans/" + objectPath)
                .addHeader("x-upsert", "true")
                .post(RequestBody.create(videoFile, VIDEO_MEDIA))
                .build();

        executeWithoutBody(request);
    }

    private void updateFilePath(@NonNull String itemId, @NonNull String objectPath)
            throws IOException, JSONException {
        JSONObject body = new JSONObject();
        body.put("file_path", objectPath);

        Request request = baseApiRequest(baseUrl + "/rest/v1/MarketplaceItems?id=eq." + itemId)
                .addHeader("Prefer", "return=minimal")
                .method("PATCH", RequestBody.create(body.toString(), JSON_MEDIA))
                .build();

        executeWithoutBody(request);
    }

    private void invokeStartKiriJob(@NonNull String itemId) throws IOException, JSONException {
        JSONObject payload = new JSONObject();
        payload.put("item_id", itemId);

        Request request = baseApiRequest(baseUrl + "/functions/v1/start-kiri-job")
                .post(RequestBody.create(payload.toString(), JSON_MEDIA))
                .build();

        executeWithoutBody(request);
    }

    @NonNull
    private MarketplaceItemRecord fetchMarketplaceItem(@NonNull String itemId)
            throws IOException, JSONException {
        Request request = baseApiRequest(baseUrl + "/rest/v1/MarketplaceItems?id=eq." + itemId + "&select=*")
                .get()
                .build();

        JSONObject object = executeForSingleObject(request);
        return parseMarketplaceItem(object);
    }

    @NonNull
    private List<MarketplaceItemRecord> fetchReadyMarketplaceItems() throws IOException, JSONException {
        Request request = baseApiRequest(baseUrl + "/rest/v1/MarketplaceItems?status=eq.READY&select=*&order=created_at.desc")
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException(errorFrom(response));
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new IOException("Server returned an empty response.");
            }

            JSONArray array = new JSONArray(responseBody.string());
            List<MarketplaceItemRecord> result = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object != null) {
                    result.add(parseMarketplaceItem(object));
                }
            }
            return result;
        }
    }

    private int fetchAvailableScans() throws IOException, JSONException {
        Log.d(TAG, "Fetching available scans via RPC get_available_credits");
        Request request = baseApiRequest(baseUrl + "/rest/v1/rpc/get_available_credits")
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create("{}", JSON_MEDIA))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String bodyText = readBodyText(response.body());
                Log.e(TAG, "Credits RPC failed: HTTP " + response.code() + " body=" + bodyText);
                throw httpError(response, bodyText);
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new IOException("Server returned an empty response.");
            }

            String rawBody = responseBody.string();
            Log.d(TAG, "Credits RPC raw response: " + rawBody);
            int parsed = parseCreditsRpcResponse(rawBody);
            Log.d(TAG, "Credits RPC parsed value: " + parsed);
            return parsed;
        }
    }

    private int parseCreditsRpcResponse(@NonNull String rawBody) throws JSONException {
        String body = rawBody.trim();
        if (body.isEmpty()) {
            return 0;
        }

        // Common scalar response for SQL functions returning an integer.
        if (body.matches("^-?\\d+$")) {
            return Math.max(0, Integer.parseInt(body));
        }

        // Possible array/object response shapes depending on PostgREST function settings.
        if (body.startsWith("[")) {
            JSONArray array = new JSONArray(body);
            if (array.length() == 0) {
                return 0;
            }
            Object first = array.get(0);
            if (first instanceof Number) {
                return Math.max(0, ((Number) first).intValue());
            }
            if (first instanceof JSONObject) {
                JSONObject object = (JSONObject) first;
                if (object.has("get_available_credits")) {
                    return Math.max(0, object.optInt("get_available_credits", 0));
                }
                JSONArray keys = object.names();
                if (keys != null && keys.length() > 0) {
                    String firstKey = keys.optString(0, "");
                    if (!firstKey.isEmpty()) {
                        return Math.max(0, object.optInt(firstKey, 0));
                    }
                }
            }
            return 0;
        }

        if (body.startsWith("{")) {
            JSONObject object = new JSONObject(body);
            if (object.has("get_available_credits")) {
                return Math.max(0, object.optInt("get_available_credits", 0));
            }
            JSONArray keys = object.names();
            if (keys != null && keys.length() > 0) {
                String firstKey = keys.optString(0, "");
                if (!firstKey.isEmpty()) {
                    return Math.max(0, object.optInt(firstKey, 0));
                }
            }
            return 0;
        }

        throw new JSONException("Unexpected RPC response format.");
    }

    @NonNull
    private Request.Builder baseApiRequest(@NonNull String url) {
        return new Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer " + anonKey)
                .addHeader("Accept", "application/json");
    }

    private void executeWithoutBody(@NonNull Request request) throws IOException {
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String bodyText = readBodyText(response.body());
                if (response.code() == 403 && bodyText.contains(CREDITS_EXHAUSTED_CODE)) {
                    throw new CreditsExhaustedException(httpErrorText(response, bodyText));
                }
                throw new IOException(httpErrorText(response, bodyText));
            }
        }
    }

    @NonNull
    private JSONObject executeForSingleObject(@NonNull Request request) throws IOException, JSONException {
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String bodyText = readBodyText(response.body());
                throw httpError(response, bodyText);
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new IOException("Server returned an empty response.");
            }

            String text = responseBody.string();
            if (text.trim().startsWith("[")) {
                JSONArray array = new JSONArray(text);
                if (array.length() == 0) {
                    throw new IOException("Server returned no rows.");
                }
                return array.getJSONObject(0);
            }
            return new JSONObject(text);
        }
    }

    private MarketplaceItemRecord parseMarketplaceItem(@NonNull JSONObject object) {
        String id = object.optString("id", "");
        String title = object.optString("title", "");
        String statusText = object.optString("status", PipelineStatus.UNKNOWN.name());
        String usedApiKeyId = nullIfBlank(object.optString("used_api_key_id", ""));
        String sellerName = nullIfBlank(object.optString("seller_name", ""));
        String location = nullIfBlank(object.optString("location", ""));
        String category = nullIfBlank(object.optString("category", ""));
        String description = nullIfBlank(object.optString("description", ""));
        Double price = nullableDouble(object, "price");

        return new MarketplaceItemRecord(
                id,
                title,
                PipelineStatus.from(statusText),
                nullIfBlank(object.optString("file_path", "")),
                nullIfBlank(object.optString("kiri_serialize", "")),
                nullIfBlank(object.optString("model_url", "")),
                nullIfBlank(object.optString("created_at", "")),
                usedApiKeyId,
                sellerName,
                location,
                category,
                description,
                price
        );
    }

    @Nullable
    private String nullIfBlank(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    @Nullable
    private Double nullableDouble(@NonNull JSONObject object, @NonNull String key) {
        if (!object.has(key) || object.isNull(key)) {
            return null;
        }
        return object.optDouble(key);
    }

    @NonNull
    private String errorFrom(@NonNull Response response) {
        String bodyText;
        try {
            bodyText = readBodyText(response.body());
        } catch (IOException ignored) {
            bodyText = "";
        }
        return httpErrorText(response, bodyText);
    }

    private IOException httpError(@NonNull Response response, @NonNull String bodyText) {
        if (response.code() == 403 && bodyText.contains(CREDITS_EXHAUSTED_CODE)) {
            return new CreditsExhaustedException(httpErrorText(response, bodyText));
        }
        return new IOException(httpErrorText(response, bodyText));
    }

    @NonNull
    private String httpErrorText(@NonNull Response response, @NonNull String bodyText) {
        return "HTTP " + response.code() + " " + response.message()
                + (bodyText.isEmpty() ? "" : (": " + new String(bodyText.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)));
    }

    @NonNull
    private String readBodyText(@Nullable ResponseBody body) throws IOException {
        if (body == null) {
            return "";
        }
        return body.string();
    }

    public static boolean isCreditsExhaustedError(@Nullable String message, @Nullable Throwable throwable) {
        if (throwable instanceof CreditsExhaustedException) {
            return true;
        }
        if (message != null && message.contains(CREDITS_EXHAUSTED_CODE)) {
            return true;
        }
        return throwable != null
                && throwable.getMessage() != null
                && throwable.getMessage().contains(CREDITS_EXHAUSTED_CODE);
    }

    @NonNull
    private String userMessage(@NonNull Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return "Unexpected network error.";
        }
        return message;
    }

    private <T> void postSuccess(@NonNull RepositoryCallback<T> callback, @Nullable T data) {
        mainHandler.post(() -> callback.onSuccess(data));
    }

    private <T> void postError(
            @NonNull RepositoryCallback<T> callback,
            @NonNull String message,
            @Nullable Throwable throwable
    ) {
        mainHandler.post(() -> callback.onError(message, throwable));
    }

    private String stripTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    @NonNull
    private File downloadAndExtractModel(@NonNull String itemId, @NonNull String modelUrl) throws IOException {
        if (modelUrl.trim().isEmpty()) {
            throw new IOException("Model URL is empty.");
        }

        File outputDir = new File(appContext.getCacheDir(), "models/" + itemId);
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Could not create model cache directory.");
        }

        File zipFile = new File(outputDir, "model.zip");
        try {
            Log.d(TAG, "Downloading model zip: itemId=" + itemId + " url=" + modelUrl + " -> " + zipFile.getAbsolutePath());
            downloadToFile(modelUrl, zipFile);
            if (!zipFile.exists() || zipFile.length() == 0) {
                throw new IOException("Downloaded model.zip is missing or empty: " + zipFile.getAbsolutePath());
            }

            File modelFile = extractPreferredModel(zipFile, outputDir);
            if (modelFile == null) {
                throw new IOException("model.zip did not contain a supported model (.splat, .splatv, .ply).");
            }
            return modelFile;
        } finally {
            if (zipFile.exists()) {
                zipFile.delete();
            }
        }
    }

    private File downloadAndExtractPly(@NonNull String itemId, @NonNull String modelUrl) throws IOException {
        if (modelUrl.trim().isEmpty()) {
            throw new IOException("Model URL is empty.");
        }

        File outputDir = new File(appContext.getCacheDir(), "models/" + itemId);
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Could not create model cache directory.");
        }

        File zipFile = new File(outputDir, "model.zip");
        try {
            Log.d(TAG, "Downloading model zip (ply): itemId=" + itemId + " url=" + modelUrl + " -> " + zipFile.getAbsolutePath());
            downloadToFile(modelUrl, zipFile);
            if (!zipFile.exists() || zipFile.length() == 0) {
                throw new IOException("Downloaded model.zip is missing or empty: " + zipFile.getAbsolutePath());
            }

            File modelFile = extractPly(zipFile, outputDir);
            if (modelFile == null) {
                throw new IOException("model.zip did not contain a .ply model.");
            }
            return modelFile;
        } finally {
            if (zipFile.exists()) {
                zipFile.delete();
            }
        }
    }

    private void downloadToFile(@NonNull String url, @NonNull File outputFile) throws IOException {
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create download directory: " + parent.getAbsolutePath());
        }

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/octet-stream")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException(errorFrom(response));
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("Server returned an empty model response.");
            }

            try (InputStream inputStream = body.byteStream();
                 FileOutputStream fileOutputStream = new FileOutputStream(outputFile, false)) {
                byte[] buffer = new byte[16 * 1024];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, read);
                }
                fileOutputStream.flush();
            }
        } catch (IOException exception) {
            Log.e(TAG, "Model download failed: url=" + url + " file=" + outputFile.getAbsolutePath(), exception);
            throw exception;
        }
    }

    @Nullable
    private File extractPreferredModel(@NonNull File zipFile, @NonNull File outputDir) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(new java.io.FileInputStream(zipFile))) {
            ZipEntry entry;
            File selectedFile = null;
            int selectedPriority = Integer.MAX_VALUE;

            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    zipInputStream.closeEntry();
                    continue;
                }

                String entryName = entry.getName();
                String lowerName = entryName.toLowerCase(Locale.US);

                int priority;
                String outputName;
                if (lowerName.endsWith(".splat")) {
                    priority = 0;
                    outputName = "model.splat";
                } else if (lowerName.endsWith(".splatv")) {
                    priority = 1;
                    outputName = "model.splatv";
                } else if (lowerName.endsWith(".ply")) {
                    priority = 2;
                    outputName = "model.ply";
                } else {
                    zipInputStream.closeEntry();
                    continue;
                }

                if (priority > selectedPriority) {
                    // A better candidate was already extracted earlier.
                    zipInputStream.closeEntry();
                    continue;
                }

                File outputFile = new File(outputDir, outputName);
                try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile, false)) {
                    byte[] buffer = new byte[16 * 1024];
                    int read;
                    while ((read = zipInputStream.read(buffer)) != -1) {
                        fileOutputStream.write(buffer, 0, read);
                    }
                    fileOutputStream.flush();
                }
                zipInputStream.closeEntry();

                selectedFile = outputFile;
                selectedPriority = priority;

                if (selectedPriority == 0) {
                    // .splat is the preferred format for gsplat viewer.
                    break;
                }
            }

            return selectedFile;
        }
    }

    @Nullable
    private File extractPly(@NonNull File zipFile, @NonNull File outputDir) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(new java.io.FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    zipInputStream.closeEntry();
                    continue;
                }

                String lowerName = entry.getName().toLowerCase(Locale.US);
                if (!lowerName.endsWith(".ply")) {
                    zipInputStream.closeEntry();
                    continue;
                }

                File outputFile = new File(outputDir, "model.ply");
                try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile, false)) {
                    byte[] buffer = new byte[16 * 1024];
                    int read;
                    while ((read = zipInputStream.read(buffer)) != -1) {
                        fileOutputStream.write(buffer, 0, read);
                    }
                    fileOutputStream.flush();
                }
                zipInputStream.closeEntry();
                return outputFile;
            }
            return null;
        }
    }

    public interface RepositoryCallback<T> {
        void onSuccess(@Nullable T data);

        void onError(@NonNull String message, @Nullable Throwable throwable);
    }

    private static final class CreditsExhaustedException extends IOException {
        CreditsExhaustedException(@NonNull String message) {
            super(message);
        }
    }

    public void updateMarketplaceItemDetailsAsync(
            @NonNull String itemId,
            @Nullable String title,
            @Nullable String sellerName,
            @Nullable String location,
            @Nullable String category,
            @Nullable String description,
            @Nullable Double price,
            @NonNull RepositoryCallback<Void> callback
    ) {
        executor.execute(() -> {
            try {
                updateMarketplaceItemDetails(itemId, title, sellerName, location, category, description, price);
                postSuccess(callback, null);
            } catch (Exception exception) {
                postError(callback, userMessage(exception), exception);
            }
        });
    }

    private void updateMarketplaceItemDetails(
            @NonNull String itemId,
            @Nullable String title,
            @Nullable String sellerName,
            @Nullable String location,
            @Nullable String category,
            @Nullable String description,
            @Nullable Double price
    ) throws IOException, JSONException {
        JSONObject body = new JSONObject();
        putOptional(body, "title", title);
        putOptional(body, "seller_name", sellerName);
        putOptional(body, "location", location);
        putOptional(body, "category", category);
        putOptional(body, "description", description);
        body.put("price", price == null ? JSONObject.NULL : price);

        Request request = baseApiRequest(baseUrl + "/rest/v1/MarketplaceItems?id=eq." + itemId)
                .addHeader("Prefer", "return=minimal")
                .method("PATCH", RequestBody.create(body.toString(), JSON_MEDIA))
                .build();

        executeWithoutBody(request);
    }

    private void putOptional(@NonNull JSONObject body, @NonNull String key, @Nullable String value) throws JSONException {
        String trimmed = value == null ? null : value.trim();
        body.put(key, trimmed == null || trimmed.isEmpty() ? JSONObject.NULL : trimmed);
    }
}
