package com.example.final_project;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.Nullable;

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

import android.util.Base64;

/**
 * Calls Gemini REST API directly — bypasses SDK 0.9.0 which fails to parse
 * responses from newer models (gemini-2.0/2.5) that include unknown JSON fields.
 */
public final class GeminiTextRunner {

    private static final String TAG = "GeminiTextRunner";
    private static final String BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final int CONNECT_MS = 20_000;
    private static final int READ_MS    = 90_000;

    private static final String[] MODELS = {
            "gemini-3.1-flash-lite-preview",
            "gemini-2.5-flash-lite",
            "gemini-3-flash-preview",
            "gemini-2.5-flash"
    };

    private GeminiTextRunner() {}

    public static String run(String prompt, @Nullable Bitmap bitmap,
                             float temperature, int maxTokens) throws Exception {
        String apiKey = BuildConfig.GEMINI_API_KEY;
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("Thiếu GEMINI_API_KEY trong local.properties");
        }

        StringBuilder errors = new StringBuilder();
        for (String model : MODELS) {
            try {
                String text = callRest(model, apiKey, prompt, bitmap, temperature, maxTokens);
                if (text != null && !text.isEmpty()) return text;
                appendError(errors, model, "phản hồi trống");
            } catch (Exception e) {
                Log.w(TAG, "Model " + model + " lỗi: " + e.getMessage());
                appendError(errors, model, e.getMessage());
            }
        }
        throw new Exception(classify(errors.toString()));
    }

    private static String callRest(String model, String apiKey, String prompt,
                                    @Nullable Bitmap bitmap,
                                    float temperature, int maxTokens) throws Exception {
        URL url = new URL(BASE_URL + model + ":generateContent?key=" + apiKey);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setDoOutput(true);
        conn.setConnectTimeout(CONNECT_MS);
        conn.setReadTimeout(READ_MS);

        JSONObject body = buildBody(prompt, bitmap, temperature, maxTokens);
        byte[] bodyBytes = body.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(bodyBytes);
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

    private static JSONObject buildBody(String prompt, @Nullable Bitmap bitmap,
                                         float temperature, int maxTokens) throws Exception {
        JSONArray parts = new JSONArray();
        parts.put(new JSONObject().put("text", prompt));

        if (bitmap != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
            String b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
            parts.put(new JSONObject()
                    .put("inlineData", new JSONObject()
                            .put("mimeType", "image/jpeg")
                            .put("data", b64)));
        }

        JSONObject content = new JSONObject()
                .put("role", "user")
                .put("parts", parts);

        JSONObject genCfg = new JSONObject()
                .put("temperature", (double) temperature)
                .put("maxOutputTokens", maxTokens);

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
        return text.toString().trim();
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
            return "API key không hợp lệ.\n" + details;
        if (d.contains("resource_exhausted") || d.contains("quota") || d.contains("429"))
            return "Vượt quota Gemini API.\n" + details;
        if (d.contains("unknownhostexception") || d.contains("unable to resolve"))
            return "Không kết nối được tới Gemini API. Kiểm tra mạng.\n" + details;
        if (d.contains("timeout") || d.contains("sockettimeout"))
            return "Gemini API timeout.\n" + details;
        return "Gemini lỗi:\n" + details;
    }

    private static void appendError(StringBuilder sb, String model, String reason) {
        if (sb.length() > 0) sb.append("\n");
        sb.append("• ").append(model).append(": ").append(reason);
    }

    /** Strip ```json ... ``` or ``` ... ``` fences from a response. */
    public static String stripCodeFence(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            if (firstNewline > 0) s = s.substring(firstNewline + 1);
            if (s.endsWith("```")) s = s.substring(0, s.length() - 3);
        }
        return s.trim();
    }
}
