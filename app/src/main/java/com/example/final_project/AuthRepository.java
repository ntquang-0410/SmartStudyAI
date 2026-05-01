package com.example.final_project;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Wraps FirebaseAuth + Firestore user-profile bootstrap.
 * Activity/ViewModel does not touch Firebase APIs directly.
 */
public class AuthRepository {

    private static final String COLL_USERS = "users";

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    public AuthRepository() {
        this.auth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
    }

    public Task<AuthResult> login(@NonNull String email, @NonNull String password) {
        return auth.signInWithEmailAndPassword(email, password);
    }

    public Task<Void> sendPasswordReset(@NonNull String email) {
        return auth.sendPasswordResetEmail(email);
    }

    /**
     * Create account, set display name, save profile doc to Firestore.
     * Resolves to Void on full success; rejects on any step failure.
     */
    public Task<Void> register(@NonNull String name,
                               @NonNull String email,
                               @NonNull String password) {
        return auth.createUserWithEmailAndPassword(email, password)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) {
                        throw new IllegalStateException("Không lấy được thông tin người dùng");
                    }
                    UserProfileChangeRequest req = new UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build();
                    return user.updateProfile(req).continueWithTask(t -> saveProfile(user, name));
                });
    }

    private Task<Void> saveProfile(FirebaseUser user, String name) {
        Map<String, Object> data = new HashMap<>();
        data.put("uid", user.getUid());
        data.put("fullName", name);
        data.put("email", user.getEmail());
        data.put("createdAt", System.currentTimeMillis());

        return firestore.collection(COLL_USERS)
                .document(user.getUid())
                .set(data);
    }
}
