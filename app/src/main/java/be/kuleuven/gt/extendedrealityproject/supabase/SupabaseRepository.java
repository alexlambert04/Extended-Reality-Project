package be.kuleuven.gt.extendedrealityproject.supabase;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    private final String baseUrl;
    private final String anonKey;
    private final OkHttpClient httpClient;
    private final ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public SupabaseRepository(@NonNull Context context) {
        this(context, new OkHttpClient());
    }

    public SupabaseRepository(@NonNull Context context, @NonNull OkHttpClient httpClient) {
        this.baseUrl = stripTrailingSlash(BuildConfig.SUPABASE_URL);
        this.anonKey = BuildConfig.SUPABASE_ANON_KEY;
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
                throw new IOException(errorFrom(response));
            }
        }
    }

    @NonNull
    private JSONObject executeForSingleObject(@NonNull Request request) throws IOException, JSONException {
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException(errorFrom(response));
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

        return new MarketplaceItemRecord(
                id,
                title,
                PipelineStatus.from(statusText),
                object.optString("file_path", ""),
                object.optString("kiri_serialize", ""),
                object.optString("model_url", ""),
                object.optString("created_at", "")
        );
    }

    @NonNull
    private String errorFrom(@NonNull Response response) {
        String bodyText = "";
        try {
            ResponseBody body = response.body();
            if (body != null) {
                bodyText = body.string();
            }
        } catch (IOException ignored) {
            bodyText = "";
        }
        return "HTTP " + response.code() + " " + response.message()
                + (bodyText.isEmpty() ? "" : (": " + new String(bodyText.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)));
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

    public interface RepositoryCallback<T> {
        void onSuccess(@Nullable T data);

        void onError(@NonNull String message, @Nullable Throwable throwable);
    }
}

