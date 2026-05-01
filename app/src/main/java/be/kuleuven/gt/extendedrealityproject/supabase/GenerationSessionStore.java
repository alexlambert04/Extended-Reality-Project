package be.kuleuven.gt.extendedrealityproject.supabase;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class GenerationSessionStore {

    private static final String PREF_NAME = "generation_session";
    private static final String KEY_ITEM_ID = "item_id";
    private static final String KEY_TITLE = "title";
    private static final String KEY_VIDEO_PATH = "video_path";

    private final SharedPreferences preferences;

    public GenerationSessionStore(@NonNull Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void save(@NonNull String itemId, @Nullable String title, @Nullable String videoPath) {
        preferences.edit()
                .putString(KEY_ITEM_ID, itemId)
                .putString(KEY_TITLE, title)
                .putString(KEY_VIDEO_PATH, videoPath)
                .apply();
    }

    public void clear() {
        preferences.edit().clear().apply();
    }

    @Nullable
    public Session load() {
        String itemId = preferences.getString(KEY_ITEM_ID, null);
        if (itemId == null || itemId.trim().isEmpty()) {
            return null;
        }
        return new Session(
                itemId,
                preferences.getString(KEY_TITLE, ""),
                preferences.getString(KEY_VIDEO_PATH, "")
        );
    }

    public static class Session {

        public final String itemId;
        public final String title;
        public final String videoPath;

        public Session(String itemId, String title, String videoPath) {
            this.itemId = itemId;
            this.title = title;
            this.videoPath = videoPath;
        }
    }
}

