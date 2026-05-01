package com.example.final_project;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.OutputStream;

public final class BitmapSaver {

    private BitmapSaver() {}

    public static boolean writeJpeg(@NonNull ContentResolver resolver,
                                    @NonNull Uri target,
                                    @Nullable Bitmap bitmap) {
        if (bitmap == null) return false;
        try (OutputStream out = resolver.openOutputStream(target)) {
            if (out == null) return false;
            return bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out);
        } catch (Exception e) {
            return false;
        }
    }
}
