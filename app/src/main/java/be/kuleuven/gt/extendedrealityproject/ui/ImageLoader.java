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

import java.io.ByteArrayOutputStream;
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

    public static void loadIntoSized(@NonNull ImageView target,
                                     @Nullable String url,
                                     @DrawableRes int placeholderRes,
                                     int targetWidthPx,
                                     int targetHeightPx) {
        if (url == null || url.trim().isEmpty()) {
            target.setImageResource(placeholderRes);
            return;
        }

        String cleanedUrl = url.trim();
        String cacheKey = cleanedUrl + "|" + targetWidthPx + "x" + targetHeightPx;
        target.setTag(cacheKey);

        Bitmap cached = CACHE.get(cacheKey);
        if (cached != null) {
            target.setImageBitmap(cached);
            return;
        }

        target.setImageResource(placeholderRes);
        EXECUTOR.execute(() -> {
            Bitmap bitmap = fetchBitmap(cleanedUrl, targetWidthPx, targetHeightPx);
            if (bitmap != null) {
                CACHE.put(cacheKey, bitmap);
                MAIN.post(() -> {
                    Object tag = target.getTag();
                    if (cacheKey.equals(tag)) {
                        target.setImageBitmap(bitmap);
                    }
                });
            }
        });
    }

    @Nullable
    private static Bitmap fetchBitmap(@NonNull String urlText, int targetWidthPx, int targetHeightPx) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlText);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setInstanceFollowRedirects(true);
            try (InputStream stream = connection.getInputStream()) {
                byte[] data = readAllBytes(stream);
                if (data.length == 0) {
                    return null;
                }
                if (targetWidthPx <= 0 || targetHeightPx <= 0) {
                    return BitmapFactory.decodeByteArray(data, 0, data.length);
                }
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(data, 0, data.length, options);
                options.inSampleSize = calculateInSampleSize(options, targetWidthPx, targetHeightPx);
                options.inJustDecodeBounds = false;
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                return BitmapFactory.decodeByteArray(data, 0, data.length, options);
            }
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
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

    private static int calculateInSampleSize(@NonNull BitmapFactory.Options options,
                                             int reqWidth,
                                             int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return Math.max(1, inSampleSize);
    }

    @NonNull
    private static byte[] readAllBytes(@NonNull InputStream stream) throws java.io.IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8 * 1024];
        int read;
        while ((read = stream.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }
}
