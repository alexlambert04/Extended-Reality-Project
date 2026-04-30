package be.kuleuven.gt.extendedrealityproject.supabase;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class SupabaseRealtimeClient {

    private static final long HEARTBEAT_MS = TimeUnit.SECONDS.toMillis(25);

    private final String websocketUrl;
    private final String anonKey;
    private final RealtimeListener listener;
    private final OkHttpClient client;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicInteger refCounter = new AtomicInteger(1);

    private WebSocket webSocket;
    private String topic;

    private final Runnable heartbeatRunnable = new Runnable() {
        @Override
        public void run() {
            sendHeartbeat();
            mainHandler.postDelayed(this, HEARTBEAT_MS);
        }
    };

    public SupabaseRealtimeClient(
            @NonNull String websocketUrl,
            @NonNull String anonKey,
            @NonNull RealtimeListener listener
    ) {
        this.websocketUrl = websocketUrl;
        this.anonKey = anonKey;
        this.listener = listener;
        this.client = new OkHttpClient.Builder().build();
    }

    public void connect(@NonNull String itemId) {
        disconnect();

        topic = "realtime:public:MarketplaceItems";

        Request request = new Request.Builder()
                .url(websocketUrl)
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                sendJoin(itemId);
                mainHandler.postDelayed(heartbeatRunnable, HEARTBEAT_MS);
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                handleMessage(text);
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, Response response) {
                mainHandler.post(() -> listener.onInfo("Realtime error: " + t.getMessage()));
            }

            @Override
            public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                mainHandler.post(() -> listener.onInfo("Realtime closed: " + reason));
            }
        });
    }

    public void disconnect() {
        mainHandler.removeCallbacks(heartbeatRunnable);
        if (webSocket != null) {
            webSocket.close(1000, "client_disconnected");
            webSocket = null;
        }
    }

    private void sendJoin(@NonNull String itemId) {
        if (webSocket == null) {
            return;
        }

        try {
            JSONObject root = new JSONObject();
            root.put("topic", topic);
            root.put("event", "phx_join");
            root.put("ref", String.valueOf(refCounter.getAndIncrement()));

            JSONObject payload = new JSONObject();
            JSONObject config = new JSONObject();
            JSONArray postgresChanges = new JSONArray();

            JSONObject change = new JSONObject();
            change.put("event", "*");
            change.put("schema", "public");
            change.put("table", "MarketplaceItems");
            change.put("filter", "id=eq." + itemId);
            postgresChanges.put(change);

            config.put("postgres_changes", postgresChanges);
            payload.put("config", config);
            payload.put("access_token", anonKey);

            root.put("payload", payload);

            webSocket.send(root.toString());
        } catch (JSONException exception) {
            listener.onInfo("Failed to join realtime channel.");
        }
    }

    private void sendHeartbeat() {
        if (webSocket == null) {
            return;
        }
        try {
            JSONObject root = new JSONObject();
            root.put("topic", "phoenix");
            root.put("event", "heartbeat");
            root.put("payload", new JSONObject());
            root.put("ref", String.valueOf(refCounter.getAndIncrement()));
            webSocket.send(root.toString());
        } catch (JSONException exception) {
            listener.onInfo("Failed to send heartbeat.");
        }
    }

    private void handleMessage(@NonNull String raw) {
        try {
            JSONObject message = new JSONObject(raw);
            String event = message.optString("event", "");
            JSONObject payload = message.optJSONObject("payload");

            if ("postgres_changes".equals(event) && payload != null) {
                JSONObject data = payload.optJSONObject("data");
                JSONObject record = data == null ? null : data.optJSONObject("record");
                if (record != null) {
                    MarketplaceItemRecord item = new MarketplaceItemRecord(
                            record.optString("id", ""),
                            record.optString("title", ""),
                            PipelineStatus.from(record.optString("status", "UNKNOWN")),
                            nullIfBlank(record.optString("file_path", "")),
                            nullIfBlank(record.optString("kiri_serialize", "")),
                            nullIfBlank(record.optString("model_url", "")),
                            nullIfBlank(record.optString("created_at", "")),
                            nullIfBlank(record.optString("used_api_key_id", "")),
                            nullIfBlank(record.optString("seller_name", "")),
                            nullIfBlank(record.optString("location", "")),
                            nullIfBlank(record.optString("category", "")),
                            nullIfBlank(record.optString("description", "")),
                            nullableDouble(record, "price")
                    );
                    mainHandler.post(() -> listener.onItemUpdate(item, raw));
                }
            } else if ("phx_reply".equals(event)) {
                mainHandler.post(() -> listener.onInfo("Realtime connected."));
            }
        } catch (JSONException ignored) {
            mainHandler.post(() -> listener.onInfo("Unexpected realtime payload."));
        }
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

    public interface RealtimeListener {
        void onItemUpdate(@NonNull MarketplaceItemRecord item, @NonNull String rawPayload);

        void onInfo(@NonNull String message);
    }
}
