package com.example.final_project;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    static final String PREFS = "notif_prefs";
    static final String KEY_LAST_SEEN = "last_seen_notification_at";

    private NotificationLogAdapter adapter;
    private ListenerRegistration registration;
    private LinearLayout llEmpty;
    private ProgressBar pbLoading;
    private TextView btnClear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notifications);

        View root = findViewById(R.id.root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        btnClear = findViewById(R.id.btn_clear);
        btnClear.setOnClickListener(v -> confirmClearAll());

        llEmpty = findViewById(R.id.ll_empty);
        pbLoading = findViewById(R.id.pb_loading);

        RecyclerView rv = findViewById(R.id.rv_notifications);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationLogAdapter();
        rv.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            llEmpty.setVisibility(View.VISIBLE);
            return;
        }

        pbLoading.setVisibility(View.VISIBLE);
        Query q = FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid())
                .collection("notification_log")
                .limit(200);

        registration = q.addSnapshotListener((snap, err) -> {
            pbLoading.setVisibility(View.GONE);
            if (err != null) {
                Toast.makeText(this,
                        "Không tải được thông báo: " + err.getMessage(),
                        Toast.LENGTH_LONG).show();
                return;
            }
            renderSnapshot(snap == null ? new ArrayList<>() : snap.getDocuments());
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (registration != null) {
            registration.remove();
            registration = null;
        }
        markAllSeen();
    }

    private void renderSnapshot(List<DocumentSnapshot> docs) {
        List<NotificationLogEntry> list = new ArrayList<>();
        for (DocumentSnapshot d : docs) {
            NotificationLogEntry e = d.toObject(NotificationLogEntry.class);
            if (e != null) {
                e.setId(d.getId());
                list.add(e);
            }
        }
        Collections.sort(list, new Comparator<NotificationLogEntry>() {
            @Override
            public int compare(NotificationLogEntry a, NotificationLogEntry b) {
                Date da = a.getFiredAt();
                Date db = b.getFiredAt();
                long ta = da == null ? 0L : da.getTime();
                long tb = db == null ? 0L : db.getTime();
                return Long.compare(tb, ta);
            }
        });

        adapter.submit(list);
        boolean empty = list.isEmpty();
        llEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        btnClear.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void markAllSeen() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        prefs.edit().putLong(KEY_LAST_SEEN, System.currentTimeMillis()).apply();
    }

    private void confirmClearAll() {
        new AlertDialog.Builder(this)
                .setTitle("Xoá tất cả thông báo")
                .setMessage("Bạn có chắc muốn xoá toàn bộ lịch sử thông báo?")
                .setPositiveButton("Xoá", (d, w) -> clearAll())
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void clearAll() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(user.getUid())
                .collection("notification_log")
                .limit(400)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) return;
                    WriteBatch batch = db.batch();
                    for (QueryDocumentSnapshot d : snap) {
                        batch.delete(d.getReference());
                    }
                    batch.commit()
                            .addOnFailureListener(e -> Toast.makeText(this,
                                    "Xoá thất bại: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show());
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Không tải được thông báo: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
    }
}
