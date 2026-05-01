package com.example.final_project;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HistoryDetailActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URL = "extra_image_url";
    public static final String EXTRA_SOLUTION = "extra_solution";
    public static final String EXTRA_DATE = "extra_date";

    private WebView webView;
    private ImageView ivImage;
    private ProgressBar imgProgress;
    private Bitmap loadedBitmap;
    private String imageUrl;
    private long itemTimestamp;

    private final ActivityResultLauncher<String> saveAsLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("image/jpeg"),
                    uri -> {
                        if (uri != null) writeBitmapToUri(uri);
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history_detail);

        View root = findViewById(R.id.root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        ivImage = findViewById(R.id.iv_image);
        TextView tvDate = findViewById(R.id.tv_date);
        ImageView btnBack = findViewById(R.id.btn_back);
        ImageView btnDownload = findViewById(R.id.btn_download);
        imgProgress = findViewById(R.id.img_progress);
        webView = findViewById(R.id.webview);

        btnBack.setOnClickListener(v -> finish());

        imageUrl = getIntent().getStringExtra(EXTRA_IMAGE_URL);
        String solution = getIntent().getStringExtra(EXTRA_SOLUTION);
        itemTimestamp = getIntent().getLongExtra(EXTRA_DATE, 0L);

        if (itemTimestamp > 0) {
            SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            tvDate.setText(fmt.format(new Date(itemTimestamp)));
        }

        loadImage();

        ivImage.setOnClickListener(v -> openFullscreen());
        btnDownload.setOnClickListener(v -> requestSave());

        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        webView.setBackgroundColor(0xFFFFFFFF);

        String html = solution == null ? "" : MarkdownToHtml.toHtml(solution);
        webView.loadDataWithBaseURL(
                "https://cdn.jsdelivr.net",
                html,
                "text/html",
                "UTF-8",
                null
        );
    }

    private void loadImage() {
        if (imageUrl == null || imageUrl.isEmpty()) return;
        imgProgress.setVisibility(View.VISIBLE);
        ImageLoader.fetch(imageUrl, new ImageLoader.Callback() {
            @Override
            public void onLoaded(@NonNull Bitmap bitmap) {
                imgProgress.setVisibility(View.GONE);
                loadedBitmap = bitmap;
                ivImage.setImageBitmap(bitmap);
            }

            @Override
            public void onError(@NonNull Exception e) {
                imgProgress.setVisibility(View.GONE);
                Toast.makeText(HistoryDetailActivity.this,
                        "Không tải được ảnh: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openFullscreen() {
        if (imageUrl == null || imageUrl.isEmpty()) {
            Toast.makeText(this, "Không có ảnh để hiển thị", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, FullscreenImageActivity.class);
        intent.putExtra(FullscreenImageActivity.EXTRA_IMAGE_URL, imageUrl);
        startActivity(intent);
    }

    private void requestSave() {
        if (loadedBitmap == null) {
            Toast.makeText(this, "Ảnh chưa tải xong, vui lòng đợi", Toast.LENGTH_SHORT).show();
            return;
        }
        long ts = itemTimestamp > 0 ? itemTimestamp : System.currentTimeMillis();
        saveAsLauncher.launch("smartstudy_" + ts + ".jpg");
    }

    private void writeBitmapToUri(Uri uri) {
        boolean ok = BitmapSaver.writeJpeg(getContentResolver(), uri, loadedBitmap);
        Toast.makeText(this,
                ok ? "Đã lưu ảnh vào thiết bị" : "Lưu ảnh thất bại",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }
}
