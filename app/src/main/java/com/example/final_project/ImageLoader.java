package com.example.final_project;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.storage.FirebaseStorage;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Loads bitmaps for ImageViews. Prefers Firebase Storage SDK for storage URLs
 * (handles auth tokens & retries) and falls back to HttpURLConnection.
 */
public final class ImageLoader {

    private static final long MAX_BYTES = 20L * 1024 * 1024;
    private static final ExecutorService POOL = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(8 * 1024 * 1024) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };

    public interface Callback {
        @MainThread void onLoaded(@NonNull Bitmap bitmap);
        @MainThread void onError(@NonNull Exception e);
    }

    private ImageLoader() {}

    public static void load(ImageView target, String url, @DrawableRes int placeholder) {
        if (target == null) return;
        target.setTag(url);
        if (url == null || url.isEmpty()) {
            target.setImageResource(placeholder);
            return;
        }
        Bitmap cached = CACHE.get(url);
        if (cached != null) {
            target.setImageBitmap(cached);
            return;
        }
        target.setImageResource(placeholder);
        fetch(url, new Callback() {
            @Override
            public void onLoaded(@NonNull Bitmap bitmap) {
                if (url.equals(target.getTag())) target.setImageBitmap(bitmap);
            }

            @Override
            public void onError(@NonNull Exception e) {
                // keep placeholder; per-call sites that need feedback use fetch() directly
            }
        });
    }

    @Nullable
    public static Bitmap fromCache(String url) {
        return url == null ? null : CACHE.get(url);
    }

    public static void fetch(String url, Callback cb) {
        if (url == null || url.isEmpty()) {
            MAIN.post(() -> cb.onError(new IllegalArgumentException("URL rỗng")));
            return;
        }
        Bitmap cached = CACHE.get(url);
        if (cached != null) {
            MAIN.post(() -> cb.onLoaded(cached));
            return;
        }
        if (url.startsWith("gs://") || url.contains("firebasestorage")) {
            FirebaseStorage.getInstance().getReferenceFromUrl(url)
                    .getBytes(MAX_BYTES)
                    .addOnSuccessListener(bytes -> {
                        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        if (bmp == null) {
                            cb.onError(new RuntimeException("Không decode được ảnh"));
                            return;
                        }
                        CACHE.put(url, bmp);
                        cb.onLoaded(bmp);
                    })
                    .addOnFailureListener(cb::onError);
            return;
        }
        POOL.execute(() -> {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(15_000);
                conn.setInstanceFollowRedirects(true);
                try (InputStream in = conn.getInputStream()) {
                    Bitmap bmp = BitmapFactory.decodeStream(in);
                    if (bmp == null) {
                        MAIN.post(() -> cb.onError(new RuntimeException("Không decode được ảnh")));
                    } else {
                        CACHE.put(url, bmp);
                        MAIN.post(() -> cb.onLoaded(bmp));
                    }
                }
            } catch (Exception e) {
                MAIN.post(() -> cb.onError(e));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }
}
