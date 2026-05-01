package com.example.final_project;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks real in-app time.
 * Call onResume() when the app comes to foreground, onPause(uid) when it leaves.
 * Sessions shorter than MIN_SECONDS are ignored (e.g. accidental unlocks).
 */
public final class StudySessionTracker {

    private static final int MIN_SECONDS = 10;
    private static long sessionStartMs = 0;

    private StudySessionTracker() {}

    public static void onResume() {
        sessionStartMs = System.currentTimeMillis();
    }

    public static void onPause(String uid) {
        if (sessionStartMs == 0) return;
        long elapsedSeconds = (System.currentTimeMillis() - sessionStartMs) / 1000;
        sessionStartMs = 0;

        if (uid == null || elapsedSeconds < MIN_SECONDS) return;

        Map<String, Object> update = new HashMap<>();
        update.put("studySeconds", FieldValue.increment(elapsedSeconds));
        update.put("sessionCount", FieldValue.increment(1));

        FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .set(update, SetOptions.merge());
    }
}
