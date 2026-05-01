package com.example.final_project;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

/**
 * Persists scan history. Image goes to Firebase Storage, metadata to Firestore.
 *
 * Layout:
 *   storage:    users/{uid}/scans/{timestamp}.jpg
 *   firestore:  users/{uid}/scans/{auto-id}
 */
public class ScanHistoryRepository {

    private static final String COLL_USERS = "users";
    private static final String COLL_SCANS = "scans";
    private static final int PREVIEW_LEN = 120;

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    private final FirebaseStorage storage;

    public ScanHistoryRepository() {
        this.auth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
    }

    @Nullable
    public String currentUid() {
        FirebaseUser user = auth.getCurrentUser();
        return user == null ? null : user.getUid();
    }

    public Query userScansQuery() {
        String uid = currentUid();
        if (uid == null) {
            // Return an empty query that yields no results.
            return firestore.collection(COLL_USERS)
                    .document("__none__")
                    .collection(COLL_SCANS)
                    .limit(1);
        }
        return firestore.collection(COLL_USERS)
                .document(uid)
                .collection(COLL_SCANS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100);
    }

    /**
     * Upload image to Storage, then write a Firestore doc that references it.
     * Returns a Task carrying the saved item (with id set).
     */
    public Task<ScanHistoryItem> save(@NonNull byte[] imageBytes,
                                      @NonNull String solution) {
        String uid = currentUid();
        if (uid == null) {
            return Tasks.forException(new IllegalStateException("Chưa đăng nhập"));
        }

        long ts = System.currentTimeMillis();
        String storagePath = COLL_USERS + "/" + uid + "/" + COLL_SCANS + "/" + ts + ".jpg";
        StorageReference imgRef = storage.getReference().child(storagePath);

        return imgRef.putBytes(imageBytes)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    return imgRef.getDownloadUrl();
                })
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    Uri downloadUri = task.getResult();
                    String url = downloadUri == null ? null : downloadUri.toString();

                    String preview = buildPreview(solution);
                    ScanHistoryItem item = new ScanHistoryItem(uid, url, storagePath,
                            solution, preview);

                    // Manually build payload so server timestamp is applied.
                    Map<String, Object> data = new HashMap<>();
                    data.put("uid", uid);
                    data.put("imageUrl", url);
                    data.put("storagePath", storagePath);
                    data.put("solution", solution);
                    data.put("preview", preview);
                    data.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

                    DocumentReference docRef = firestore.collection(COLL_USERS)
                            .document(uid)
                            .collection(COLL_SCANS)
                            .document();

                    return docRef.set(data).continueWith(t -> {
                        if (!t.isSuccessful() && t.getException() != null) {
                            throw t.getException();
                        }
                        item.setId(docRef.getId());
                        return item;
                    });
                });
    }

    public Task<Void> updateSubject(@NonNull String docId, @NonNull String subject) {
        String uid = currentUid();
        if (uid == null) return Tasks.forException(new IllegalStateException("Chưa đăng nhập"));
        return firestore.collection(COLL_USERS)
                .document(uid)
                .collection(COLL_SCANS)
                .document(docId)
                .update("subject", subject);
    }

    public Task<Void> delete(@NonNull ScanHistoryItem item) {
        String uid = currentUid();
        if (uid == null || item.getId() == null) {
            return Tasks.forException(new IllegalStateException("Không xác định"));
        }
        Task<Void> docDelete = firestore.collection(COLL_USERS)
                .document(uid)
                .collection(COLL_SCANS)
                .document(item.getId())
                .delete();

        if (item.getStoragePath() != null && !item.getStoragePath().isEmpty()) {
            // Best-effort: ignore storage delete errors (e.g., file already gone).
            storage.getReference().child(item.getStoragePath()).delete();
        }
        return docDelete;
    }

    private static String buildPreview(@NonNull String solution) {
        String stripped = solution
                .replaceAll("\\s+", " ")
                .replaceAll("[#*`>_~\\[\\](){}]", "")
                .trim();
        if (stripped.length() <= PREVIEW_LEN) return stripped;
        return stripped.substring(0, PREVIEW_LEN) + "…";
    }
}
