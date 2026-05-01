package com.example.final_project;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class ReminderRepository {

    private static final String COLL_USERS = "users";
    private static final String COLL_REMINDERS = "reminders";

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    @Nullable
    public String currentUid() {
        return auth.getCurrentUser() == null ? null : auth.getCurrentUser().getUid();
    }

    public Query userRemindersQuery() {
        String uid = currentUid();
        if (uid == null) {
            return firestore.collection(COLL_USERS)
                    .document("__none__")
                    .collection(COLL_REMINDERS)
                    .limit(1);
        }
        // No orderBy: sorting happens client-side (avoids Firestore composite index).
        return firestore.collection(COLL_USERS)
                .document(uid)
                .collection(COLL_REMINDERS)
                .limit(100);
    }

    public Task<Reminder> save(@NonNull Reminder reminder) {
        String uid = currentUid();
        if (uid == null) return Tasks.forException(new IllegalStateException("Chưa đăng nhập"));

        DocumentReference doc;
        if (reminder.getId() == null || reminder.getId().isEmpty()) {
            doc = firestore.collection(COLL_USERS)
                    .document(uid)
                    .collection(COLL_REMINDERS)
                    .document();
        } else {
            doc = firestore.collection(COLL_USERS)
                    .document(uid)
                    .collection(COLL_REMINDERS)
                    .document(reminder.getId());
        }
        reminder.setUid(uid);

        return doc.set(reminder).continueWith(t -> {
            if (!t.isSuccessful() && t.getException() != null) throw t.getException();
            reminder.setId(doc.getId());
            return reminder;
        });
    }

    public Task<Void> setEnabled(@NonNull String reminderId, boolean enabled) {
        String uid = currentUid();
        if (uid == null) return Tasks.forException(new IllegalStateException("Chưa đăng nhập"));
        return firestore.collection(COLL_USERS)
                .document(uid)
                .collection(COLL_REMINDERS)
                .document(reminderId)
                .update("enabled", enabled);
    }

    public Task<Void> delete(@NonNull String reminderId) {
        String uid = currentUid();
        if (uid == null) return Tasks.forException(new IllegalStateException("Chưa đăng nhập"));
        return firestore.collection(COLL_USERS)
                .document(uid)
                .collection(COLL_REMINDERS)
                .document(reminderId)
                .delete();
    }
}
