package com.example.final_project;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class QuizRepository {

    private static final String COLL_USERS = "users";
    private static final String COLL_QUIZZES = "quizzes";

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    @Nullable
    public String currentUid() {
        return auth.getCurrentUser() == null ? null : auth.getCurrentUser().getUid();
    }

    public Query userQuizzesQuery() {
        String uid = currentUid();
        if (uid == null) {
            return firestore.collection(COLL_USERS)
                    .document("__none__")
                    .collection(COLL_QUIZZES)
                    .limit(1);
        }
        return firestore.collection(COLL_USERS)
                .document(uid)
                .collection(COLL_QUIZZES)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100);
    }

    public Task<Quiz> save(@NonNull Quiz quiz) {
        String uid = currentUid();
        if (uid == null) return Tasks.forException(new IllegalStateException("Chưa đăng nhập"));
        DocumentReference doc = firestore.collection(COLL_USERS)
                .document(uid)
                .collection(COLL_QUIZZES)
                .document();
        quiz.setUid(uid);
        return doc.set(quiz).continueWith(t -> {
            if (!t.isSuccessful() && t.getException() != null) throw t.getException();
            quiz.setId(doc.getId());
            return quiz;
        });
    }

    public Task<Void> delete(@NonNull String quizId) {
        String uid = currentUid();
        if (uid == null) return Tasks.forException(new IllegalStateException("Chưa đăng nhập"));
        return firestore.collection(COLL_USERS)
                .document(uid)
                .collection(COLL_QUIZZES)
                .document(quizId)
                .delete();
    }
}
