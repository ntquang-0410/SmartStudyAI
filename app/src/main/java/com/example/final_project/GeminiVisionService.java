package com.example.final_project;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Sends image + prompt to Gemini REST API directly.
 * Avoids SDK 0.9.0 which fails to deserialize responses from newer models
 * (gemini-2.0/2.5) that contain JSON fields unknown to the old SDK.
 */
public class GeminiVisionService {

    private static final String TAG = "GeminiVisionService";
    private static final String API_KEY = BuildConfig.GEMINI_API_KEY;
    private static final String BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    private static final String[] MODELS = {
            "gemini-3.1-flash-lite-preview",
            "gemini-2.5-flash-lite",
            "gemini-3-flash-preview",
            "gemini-2.5-flash"
    };

    private static final int MAX_IMAGE_SIZE = 1024;
    private static final int CONNECT_MS     = 20_000;
    private static final int READ_MS        = 90_000;

    private final Context context;

    public GeminiVisionService(Context context) {
        this.context = context.getApplicationContext();
    }

    // ── Public API ────────────────────────────────────────────────────────────


    public GeminiResult solveFromBitmap(Bitmap bitmap, String extraText) {
        try {
            if (API_KEY == null || API_KEY.trim().isEmpty()) {
                return new GeminiResult.Error(
                        "Thiếu GEMINI_API_KEY. Vui lòng cấu hình trong local.properties.");
            }
            return solve(resizeBitmap(bitmap), extraText);
        } catch (Exception e) {
            Log.e(TAG, "solveFromBitmap failed", e);
            return new GeminiResult.Error(e.getMessage() != null ? e.getMessage() : "Lỗi không xác định");
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private GeminiResult solve(Bitmap bitmap, String extraText) {
        String prompt = buildPrompt(extraText);
        String b64Image = bitmapToBase64(bitmap);

        StringBuilder errors = new StringBuilder();
        for (String model : MODELS) {
            try {
                String text = callRest(model, prompt, b64Image);
                if (text != null && !text.isEmpty()) return new GeminiResult.Success(text);
                appendError(errors, model, "phản hồi trống");
            } catch (Exception e) {
                Log.w(TAG, "Model " + model + " lỗi: " + e.getMessage());
                appendError(errors, model, e.getMessage());
            }
        }
        return new GeminiResult.Error(classify(errors.toString()));
    }

    private String callRest(String model, String prompt, String b64Image) throws Exception {
        URL url = new URL(BASE_URL + model + ":generateContent?key=" + API_KEY);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setDoOutput(true);
        conn.setConnectTimeout(CONNECT_MS);
        conn.setReadTimeout(READ_MS);

        JSONObject body = buildBody(prompt, b64Image);
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(bytes);
        }

        int code = conn.getResponseCode();
        InputStream is = (code >= 400) ? conn.getErrorStream() : conn.getInputStream();
        if (is == null) throw new Exception("HTTP " + code + " (không có response body)");

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        conn.disconnect();

        if (code >= 400) {
            throw new Exception("HTTP " + code + ": " + extractApiError(sb.toString()));
        }
        return extractText(sb.toString());
    }

    private static JSONObject buildBody(String prompt, String b64Image) throws Exception {
        JSONArray parts = new JSONArray();
        parts.put(new JSONObject().put("text", prompt));
        parts.put(new JSONObject()
                .put("inlineData", new JSONObject()
                        .put("mimeType", "image/jpeg")
                        .put("data", b64Image)));

        JSONObject content = new JSONObject()
                .put("role", "user")
                .put("parts", parts);

        JSONObject genCfg = new JSONObject()
                .put("temperature", 0.2)
                .put("maxOutputTokens", 8192);

        return new JSONObject()
                .put("contents", new JSONArray().put(content))
                .put("generationConfig", genCfg);
    }

    private static String extractText(String body) throws Exception {
        JSONObject resp = new JSONObject(body);

        JSONArray candidates = resp.optJSONArray("candidates");
        if (candidates == null || candidates.length() == 0) {
            JSONObject fb = resp.optJSONObject("promptFeedback");
            String reason = (fb != null) ? fb.optString("blockReason", "unknown") : "no candidates";
            throw new Exception("Bị chặn: " + reason);
        }

        JSONObject candidate = candidates.getJSONObject(0);
        JSONObject content = candidate.optJSONObject("content");
        if (content == null) {
            throw new Exception("Không có content. finishReason="
                    + candidate.optString("finishReason"));
        }

        JSONArray parts = content.optJSONArray("parts");
        if (parts == null || parts.length() == 0) {
            throw new Exception("Không có parts");
        }

        StringBuilder text = new StringBuilder();
        for (int i = 0; i < parts.length(); i++) {
            text.append(parts.getJSONObject(i).optString("text", ""));
        }
        String result = text.toString().trim();
        if (result.isEmpty()) throw new Exception("Text rỗng");
        return result;
    }

    private static String extractApiError(String body) {
        try {
            JSONObject err = new JSONObject(body).optJSONObject("error");
            if (err != null) return err.optString("message", body);
        } catch (Exception ignored) {}
        return body.length() > 200 ? body.substring(0, 200) : body;
    }

    private static String classify(String details) {
        String d = details.toLowerCase();
        if (d.contains("api key") || d.contains("api_key") || d.contains("invalid_argument"))
            return "API key không hợp lệ. Kiểm tra lại GEMINI_API_KEY trong local.properties.";
        if (d.contains("resource_exhausted") || d.contains("quota") || d.contains("429"))
            return "Đã vượt quota Gemini API. Thử lại sau.";
        if (d.contains("unknownhostexception") || d.contains("unable to resolve"))
            return "Không kết nối được tới Gemini API. Kiểm tra mạng.";
        if (d.contains("timeout") || d.contains("sockettimeout"))
            return "Gemini API không phản hồi (timeout). Kiểm tra mạng hoặc thử lại.";
        return "Lỗi Gemini:\n" + details;
    }

    private static void appendError(StringBuilder sb, String model, String reason) {
        if (sb.length() > 0) sb.append("\n");
        sb.append("• ").append(model).append(": ").append(reason);
    }

    // ── Image helpers ─────────────────────────────────────────────────────────

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
        if (src.getWidth() <= MAX_IMAGE_SIZE && src.getHeight() <= MAX_IMAGE_SIZE) return src;
        float ratio = Math.min(
                (float) MAX_IMAGE_SIZE / src.getWidth(),
                (float) MAX_IMAGE_SIZE / src.getHeight());
        return Bitmap.createScaledBitmap(src,
                (int) (src.getWidth() * ratio),
                (int) (src.getHeight() * ratio), true);
    }

    private static String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
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
}
