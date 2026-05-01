package com.example.final_project;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class FullscreenImageActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URL = "extra_image_url";

    private ImageView ivFull;
    private ProgressBar progress;
    private Bitmap loadedBitmap;
    private long timestamp;

    private final ActivityResultLauncher<String> saveAsLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("image/jpeg"),
                    uri -> {
                        if (uri != null) writeBitmapToUri(uri);
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_fullscreen_image);

        View root = findViewById(R.id.root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        ivFull = findViewById(R.id.iv_full);
        progress = findViewById(R.id.progress);
        ImageView btnClose = findViewById(R.id.btn_close);
        ImageView btnDownload = findViewById(R.id.btn_download_full);

        btnClose.setOnClickListener(v -> finish());
        ivFull.setOnClickListener(v -> finish());

        timestamp = System.currentTimeMillis();
        String url = getIntent().getStringExtra(EXTRA_IMAGE_URL);

        progress.setVisibility(View.VISIBLE);
        ImageLoader.fetch(url, new ImageLoader.Callback() {
            @Override
            public void onLoaded(@androidx.annotation.NonNull Bitmap bitmap) {
                loadedBitmap = bitmap;
                progress.setVisibility(View.GONE);
                ivFull.setImageBitmap(bitmap);
            }

            @Override
            public void onError(@androidx.annotation.NonNull Exception e) {
                progress.setVisibility(View.GONE);
                Toast.makeText(FullscreenImageActivity.this,
                        "Không tải được ảnh: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });

        btnDownload.setOnClickListener(v -> requestSave());
    }

    private void requestSave() {
        if (loadedBitmap == null) {
            Toast.makeText(this, "Ảnh chưa tải xong", Toast.LENGTH_SHORT).show();
            return;
        }
        saveAsLauncher.launch("smartstudy_" + timestamp + ".jpg");
    }

    private void writeBitmapToUri(Uri uri) {
        boolean ok = BitmapSaver.writeJpeg(getContentResolver(), uri, loadedBitmap);
        Toast.makeText(this,
                ok ? "Đã lưu ảnh" : "Lưu ảnh thất bại",
                Toast.LENGTH_SHORT).show();
    }
}
