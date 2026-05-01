package com.example.final_project;

import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

/**
 * Renders a subject chip on a TextView. Each subject gets its own
 * background/text colour for instant visual recognition.
 */
public final class SubjectChipUi {

    private SubjectChipUi() {}

    public static void apply(@NonNull TextView tv, String subject) {
        if (subject == null || subject.isEmpty()) {
            tv.setVisibility(android.view.View.GONE);
            return;
        }
        tv.setVisibility(android.view.View.VISIBLE);
        tv.setText(subject);

        int bg = bgFor(subject);
        int fg = fgFor(subject);

        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setColor(bg);
        shape.setCornerRadius(dp(tv, 50));
        tv.setBackground(shape);
        tv.setTextColor(fg);

        int padH = dp(tv, 10);
        int padV = dp(tv, 3);
        tv.setPadding(padH, padV, padH, padV);
    }

    @ColorInt
    private static int bgFor(@NonNull String subject) {
        switch (subject) {
            case SubjectClassifier.MATH:        return 0xFFE0F2FE; // sky-100
            case SubjectClassifier.LITERATURE:  return 0xFFFCE7F3; // pink-100
            case SubjectClassifier.ENGLISH:     return 0xFFEDE9FE; // violet-100
            case SubjectClassifier.PHYSICS:     return 0xFFDBEAFE; // blue-100
            case SubjectClassifier.CHEMISTRY:   return 0xFFDCFCE7; // green-100
            case SubjectClassifier.BIOLOGY:     return 0xFFD1FAE5; // emerald-100
            case SubjectClassifier.HISTORY:     return 0xFFFEF3C7; // amber-100
            case SubjectClassifier.GEOGRAPHY:   return 0xFFFFEDD5; // orange-100
            case SubjectClassifier.CIVICS:      return 0xFFFEE2E2; // red-100
            case SubjectClassifier.INFORMATICS: return 0xFFCFFAFE; // cyan-100
            default:                            return 0xFFE2E8F0; // slate-200
        }
    }

    @ColorInt
    private static int fgFor(@NonNull String subject) {
        switch (subject) {
            case SubjectClassifier.MATH:        return 0xFF0369A1;
            case SubjectClassifier.LITERATURE:  return 0xFFBE185D;
            case SubjectClassifier.ENGLISH:     return 0xFF6D28D9;
            case SubjectClassifier.PHYSICS:     return 0xFF1D4ED8;
            case SubjectClassifier.CHEMISTRY:   return 0xFF15803D;
            case SubjectClassifier.BIOLOGY:     return 0xFF047857;
            case SubjectClassifier.HISTORY:     return 0xFFB45309;
            case SubjectClassifier.GEOGRAPHY:   return 0xFFC2410C;
            case SubjectClassifier.CIVICS:      return 0xFFB91C1C;
            case SubjectClassifier.INFORMATICS: return 0xFF0E7490;
            default:                            return 0xFF475569;
        }
    }

    private static int dp(TextView tv, int v) {
        return Math.round(v * tv.getResources().getDisplayMetrics().density);
    }
}
