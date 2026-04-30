package com.example.final_project;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.InputStream;
import java.util.concurrent.ExecutionException;

public class GeminiVisionService {

    // ── Constants ─────────────────────────────────────────────────────────────

    private static final String TAG = "GeminiVisionService";

    private static final String API_KEY       = BuildConfig.GEMINI_API_KEY;
    // Official model names
    private static final String[] MODEL_CANDIDATES = {
            "gemini-3-flash-preview",
            "gemini-2.0-flash",
            "gemini-2.0-flash-lite-preview"
    };

    private static final int MAX_IMAGE_SIZE   = 1024;   // px

    // ── Fields ────────────────────────────────────────────────────────────────

    private final Context context;

    // ── Constructor ───────────────────────────────────────────────────────────

    public GeminiVisionService(Context context) {
        this.context = context.getApplicationContext();
    }

    // ── Public API ────────────────────────────────────────────────────────────
    public GeminiResult solveFromUri(Uri imageUri, String extraText) {
        try {
            if (API_KEY == null || API_KEY.trim().isEmpty()) {
                return new GeminiResult.Error("Thiếu GEMINI_API_KEY. Vui lòng cấu hình trong local.properties.");
            }
            Bitmap bitmap = loadAndResizeBitmap(imageUri);
            if (bitmap == null) return new GeminiResult.Error("Không đọc được ảnh");

            String prompt       = buildPrompt(extraText);
            String answer       = generateWithFallback(bitmap, prompt);

            return new GeminiResult.Success(answer);
        } catch (Exception e) {
            Log.e(TAG, "solveFromUri failed", e);
            String msg = e.getMessage();
            if (e.getCause() != null) msg += "\nNguyên nhân: " + e.getCause().getMessage();
            return new GeminiResult.Error(msg != null ? msg : "Lỗi không xác định");
        }
    }

    public GeminiResult solveFromBitmap(Bitmap bitmap, String extraText) {
        try {
            if (API_KEY == null || API_KEY.trim().isEmpty()) {
                return new GeminiResult.Error("Thiếu GEMINI_API_KEY. Vui lòng cấu hình trong local.properties.");
            }
            Bitmap resized      = resizeBitmap(bitmap);
            String prompt       = buildPrompt(extraText);
            String answer       = generateWithFallback(resized, prompt);

            return new GeminiResult.Success(answer);
        } catch (Exception e) {
            Log.e(TAG, "solveFromBitmap failed", e);
            String msg = e.getMessage();
            if (e.getCause() != null) msg += "\nNguyên nhân: " + e.getCause().getMessage();
            return new GeminiResult.Error(msg != null ? msg : "Lỗi không xác định");
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private Bitmap loadAndResizeBitmap(Uri uri) {
        try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
            if (stream == null) return null;
            Bitmap original = BitmapFactory.decodeStream(stream);
            if (original == null) return null;
            return resizeBitmap(original);
        } catch (Exception e) {
            Log.e(TAG, "loadAndResizeBitmap failed", e);
            return null;
        }
    }

    private Bitmap resizeBitmap(Bitmap src) {
        if (src.getWidth() <= MAX_IMAGE_SIZE && src.getHeight() <= MAX_IMAGE_SIZE) {
            return src;
        }
        float ratio = Math.min(
                (float) MAX_IMAGE_SIZE / src.getWidth(),
                (float) MAX_IMAGE_SIZE / src.getHeight()
        );
        int newW = (int) (src.getWidth()  * ratio);
        int newH = (int) (src.getHeight() * ratio);
        return Bitmap.createScaledBitmap(src, newW, newH, true);
    }

    private String buildPrompt(String extraText) {
        String base =
                "Bạn là gia sư thông minh. Hãy đọc bài toán trong ảnh và giải theo các bước sau:\n\n" +
                        "1. **Đề bài** – Viết lại đề bài ngắn gọn.\n" +
                        "2. **Phân tích** – Xác định dạng bài, công thức cần dùng.\n" +
                        "3. **Lời giải chi tiết** – Trình bày từng bước, dùng ký hiệu toán rõ ràng (LaTeX inline với $...$ nếu cần).\n" +
                        "4. **Kết quả** – In đậm đáp án cuối.\n" +
                        "5. **Mẹo nhớ** – 1–2 câu gợi ý để học sinh nhớ lâu.\n\n" +
                        "Trả lời bằng tiếng Việt. Nếu ảnh không chứa bài toán, hãy nói rõ.";

        if (extraText == null || extraText.trim().isEmpty()) return base;
        return base + "\n\nThông tin bổ sung từ học sinh: " + extraText.trim();
    }

    /** Generate content via Google AI SDK with model fallback. */
    private String generateWithFallback(Bitmap bitmap, String prompt) throws Exception {
        Exception lastError = null;
        StringBuilder attempted = new StringBuilder();

        GenerationConfig.Builder configBuilder = new GenerationConfig.Builder();
        configBuilder.temperature = 0.2f;
        configBuilder.maxOutputTokens = 8192;
        GenerationConfig config = configBuilder.build();

        Content content = new Content.Builder()
                .addText(prompt)
                .addImage(bitmap)
                .build();

        for (String modelName : MODEL_CANDIDATES) {
            if (attempted.length() > 0) attempted.append(", ");
            attempted.append(modelName);

            try {
                GenerativeModel gm = new GenerativeModel(modelName, API_KEY, config);
                GenerativeModelFutures model = GenerativeModelFutures.from(gm);

                ListenableFuture<GenerateContentResponse> responseFuture = model.generateContent(content);
                GenerateContentResponse response = responseFuture.get(); // Blocks on background thread
                return extractText(response);
            } catch (ExecutionException | InterruptedException e) {
                lastError = e;
                String detail = e.getMessage();
                if (e.getCause() != null) detail = e.getCause().getMessage();
                Log.w(TAG, "Model " + modelName + " failed: " + detail);
            }
        }

        String errorMsg = "Lỗi Gemini API. Đã thử: " + attempted + ".\n"
                + "Chi tiết lỗi cuối: " + (lastError != null ? (lastError.getCause() != null ? lastError.getCause().getMessage() : lastError.getMessage()) : "Unknown");
        throw new Exception(errorMsg);
    }

    private String extractText(GenerateContentResponse response) {
        if (response == null || response.getText() == null) {
            return "Gemini không trả về kết quả.";
        }
        String text = response.getText().trim();
        return text.isEmpty() ? "Gemini không trả về kết quả." : text;
    }
}
