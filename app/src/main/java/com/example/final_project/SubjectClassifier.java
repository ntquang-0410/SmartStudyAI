package com.example.final_project;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Classifies a solved problem into a school subject by asking Gemini.
 * The list of allowed subjects is closed — anything Gemini returns that
 * doesn't match falls back to {@link #OTHER}.
 */
public final class SubjectClassifier {

    public static final String MATH    = "Toán";
    public static final String LITERATURE = "Văn";
    public static final String ENGLISH = "Anh";
    public static final String PHYSICS = "Lý";
    public static final String CHEMISTRY = "Hoá";
    public static final String BIOLOGY = "Sinh";
    public static final String HISTORY = "Sử";
    public static final String GEOGRAPHY = "Địa";
    public static final String CIVICS  = "GDCD";
    public static final String INFORMATICS = "Tin";
    public static final String OTHER   = "Khác";

    public static final String[] ALL = {
            MATH, LITERATURE, ENGLISH, PHYSICS, CHEMISTRY, BIOLOGY,
            HISTORY, GEOGRAPHY, CIVICS, INFORMATICS, OTHER
    };

    private static final Set<String> VALID = new HashSet<>(Arrays.asList(ALL));
    private static final ExecutorService POOL = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public interface Callback {
        @MainThread void onClassified(@NonNull String subject);
    }

    private SubjectClassifier() {}

    public static void classifyAsync(@NonNull String solutionText, @NonNull Callback cb) {
        POOL.execute(() -> {
            String subject = classifyBlocking(solutionText);
            MAIN.post(() -> cb.onClassified(subject));
        });
    }

    private static String classifyBlocking(@NonNull String solutionText) {
        String snippet = solutionText.length() > 600
                ? solutionText.substring(0, 600)
                : solutionText;
        String prompt =
                "Phân loại bài học sau vào ĐÚNG MỘT môn học, chọn từ danh sách: "
                + "Toán, Văn, Anh, Lý, Hoá, Sinh, Sử, Địa, GDCD, Tin, Khác.\n"
                + "Chỉ trả về tên môn, không giải thích, không dấu chấm.\n\n"
                + "Bài:\n" + snippet;
        try {
            String raw = GeminiTextRunner.run(prompt, null, 0.0f, 16);
            return normalize(raw);
        } catch (Exception e) {
            return OTHER;
        }
    }

    private static String normalize(String raw) {
        if (raw == null) return OTHER;
        String s = raw.trim().replaceAll("[\\s.\\-:]+$", "");
        if (VALID.contains(s)) return s;
        // Try case-insensitive + diacritic-tolerant by direct comparison.
        for (String candidate : ALL) {
            if (s.equalsIgnoreCase(candidate)) return candidate;
        }
        // Map common synonyms / English answers from the model.
        String low = s.toLowerCase(Locale.ROOT);
        if (low.contains("math")) return MATH;
        if (low.contains("liter") || low.contains("ngữ văn")) return LITERATURE;
        if (low.contains("eng") || low.contains("tiếng anh")) return ENGLISH;
        if (low.contains("phys") || low.contains("vật lý")) return PHYSICS;
        if (low.contains("chem") || low.contains("hóa") || low.contains("hoá")) return CHEMISTRY;
        if (low.contains("bio") || low.contains("sinh")) return BIOLOGY;
        if (low.contains("hist") || low.contains("lịch sử")) return HISTORY;
        if (low.contains("geo") || low.contains("địa")) return GEOGRAPHY;
        if (low.contains("civic") || low.contains("gdcd") || low.contains("công dân")) return CIVICS;
        if (low.contains("info") || low.contains("tin học")) return INFORMATICS;
        return OTHER;
    }
}
