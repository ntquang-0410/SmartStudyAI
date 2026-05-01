package com.example.final_project;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Asks Gemini to produce a JSON quiz from a piece of solved content.
 * The model is constrained by a strict prompt template; the response is
 * stripped of code fences before JSON parsing.
 */
public class QuizGeneratorService {

    private static final int MAX_CONTENT_CHARS = 3000;

    public Quiz generate(@NonNull String sourceContent,
                         @NonNull String sourceScanId,
                         @NonNull String uid,
                         int questionCount) throws Exception {
        String trimmedContent = sourceContent.length() > MAX_CONTENT_CHARS
                ? sourceContent.substring(0, MAX_CONTENT_CHARS) + "..."
                : sourceContent;
        String prompt = buildPrompt(trimmedContent, questionCount);
        // ~300 tokens per question (concise explanations) + 512 overhead, capped at 8192
        int maxTokens = Math.min(questionCount * 300 + 512, 8192);
        String raw = GeminiTextRunner.run(prompt, null, 0.4f, maxTokens);
        String json = GeminiTextRunner.stripCodeFence(raw);

        JSONObject root;
        try {
            root = new JSONObject(json);
        } catch (Exception e) {
            throw new Exception("Gemini trả về định dạng không hợp lệ. Hãy thử lại.");
        }
        String title = root.optString("title", "Quiz");

        JSONArray arr = root.getJSONArray("questions");
        List<QuizQuestion> questions = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject q = arr.getJSONObject(i);
            JSONArray opts = q.getJSONArray("options");
            List<String> options = new ArrayList<>();
            for (int j = 0; j < opts.length(); j++) options.add(opts.getString(j));

            int correctIndex = q.optInt("correctIndex", 0);
            if (correctIndex < 0 || correctIndex >= options.size()) correctIndex = 0;

            questions.add(new QuizQuestion(
                    q.getString("question"),
                    options,
                    correctIndex,
                    q.optString("explanation", "")));
        }

        if (questions.isEmpty()) {
            throw new IllegalStateException("Gemini không trả về câu hỏi nào");
        }

        return new Quiz(uid, title, sourceScanId, questions);
    }

    private String buildPrompt(String content, int n) {
        return "Bạn là gia sư AI. Dựa trên nội dung lời giải dưới đây, hãy soạn "
                + n + " câu hỏi trắc nghiệm để học sinh ôn tập kiến thức. "
                + "Mỗi câu có 4 đáp án (A-D), chỉ một đáp án đúng. "
                + "Hỏi xen kẽ giữa lý thuyết, công thức và ứng dụng tính toán.\n\n"
                + "Nội dung:\n\"\"\"\n" + content + "\n\"\"\"\n\n"
                + "Trả về CHÍNH XÁC định dạng JSON sau (không thêm văn bản nào khác, không markdown):\n"
                + "{\n"
                + "  \"title\": \"Quiz: <chủ đề ngắn gọn>\",\n"
                + "  \"questions\": [\n"
                + "    {\n"
                + "      \"question\": \"...\",\n"
                + "      \"options\": [\"...\", \"...\", \"...\", \"...\"],\n"
                + "      \"correctIndex\": 0,\n"
                + "      \"explanation\": \"Giải thích tối đa 20 từ\"\n"
                + "    }\n"
                + "  ]\n"
                + "}\n"
                + "correctIndex là số nguyên 0-3, ứng với đáp án đúng trong mảng options.\n"
                + "Giải thích (explanation) phải ngắn gọn, tối đa 20 từ.\n"
                + "Trả lời bằng tiếng Việt.";
    }
}
