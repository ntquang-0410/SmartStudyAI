package com.example.final_project;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class UserProfileRepository {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Nullable
    public String currentUid() {
        return auth.getCurrentUser() == null ? null : auth.getCurrentUser().getUid();
    }

    public Task<UserProfile> load() {
        String uid = currentUid();
        if (uid == null) return Tasks.forException(new IllegalStateException("Chưa đăng nhập"));
        return db.collection("users").document(uid).get()
                .continueWith(t -> {
                    if (!t.isSuccessful() && t.getException() != null) throw t.getException();
                    UserProfile p = t.getResult().toObject(UserProfile.class);
                    return p == null ? new UserProfile() : p;
                });
    }

    public Task<Void> save(@NonNull UserProfile profile) {
        String uid = currentUid();
        if (uid == null) return Tasks.forException(new IllegalStateException("Chưa đăng nhập"));

        Map<String, Object> data = new HashMap<>();
        data.put("displayName", profile.getDisplayName());
        data.put("photoUrl", profile.getPhotoUrl());
        data.put("birthDate", profile.getBirthDate());
        data.put("school", profile.getSchool());
        data.put("eduLevel", profile.getEduLevel());
        data.put("grade", profile.getGrade());

        return db.collection("users").document(uid)
                .set(data, SetOptions.merge());
    }

    public Task<String> uploadAvatar(@NonNull Uri localUri) {
        String uid = currentUid();
        if (uid == null) return Tasks.forException(new IllegalStateException("Chưa đăng nhập"));
        StorageReference ref = FirebaseStorage.getInstance().getReference()
                .child("avatars").child(uid + ".jpg");
        return ref.putFile(localUri).continueWithTask(t -> {
            if (!t.isSuccessful() && t.getException() != null) throw t.getException();
            return ref.getDownloadUrl();
        }).continueWith(t -> {
            if (!t.isSuccessful() && t.getException() != null) throw t.getException();
            return t.getResult().toString();
        });
    }
}
