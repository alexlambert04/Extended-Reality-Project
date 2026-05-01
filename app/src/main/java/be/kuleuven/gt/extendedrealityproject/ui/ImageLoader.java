package be.kuleuven.gt.extendedrealityproject.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ImageLoader {

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final LruCache<String, Bitmap> CACHE = new LruCache<>(32 * 1024 * 1024);

    private ImageLoader() {
        // Utility class
    }

    public static void loadInto(@NonNull ImageView target, @Nullable String url, @DrawableRes int placeholderRes) {
        if (url == null || url.trim().isEmpty()) {
            target.setImageResource(placeholderRes);
            return;
        }

        String cleanedUrl = url.trim();
        target.setTag(cleanedUrl);

        Bitmap cached = CACHE.get(cleanedUrl);
        if (cached != null) {
            target.setImageBitmap(cached);
            return;
        }

        target.setImageResource(placeholderRes);
        EXECUTOR.execute(() -> {
            Bitmap bitmap = fetchBitmap(cleanedUrl);
            if (bitmap != null) {
                CACHE.put(cleanedUrl, bitmap);
                MAIN.post(() -> {
                    Object tag = target.getTag();
                    if (cleanedUrl.equals(tag)) {
                        target.setImageBitmap(bitmap);
                    }
                });
            }
        });
    }

    @Nullable
    private static Bitmap fetchBitmap(@NonNull String urlText) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlText);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setInstanceFollowRedirects(true);
            try (InputStream stream = connection.getInputStream()) {
                return BitmapFactory.decodeStream(stream);
            }
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}

