package com.example.final_project;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanPickerActivity extends AppCompatActivity {

    private final ScanHistoryRepository scanRepo = new ScanHistoryRepository();
    private final QuizRepository quizRepo = new QuizRepository();
    private final QuizGeneratorService generator = new QuizGeneratorService();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private ScanHistoryAdapter adapter;
    private ListenerRegistration registration;
    private View loadingOverlay;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_scan_picker);

        View root = findViewById(R.id.root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        loadingOverlay = findViewById(R.id.loading_overlay);
        tvEmpty = findViewById(R.id.tv_empty);
        RecyclerView rv = findViewById(R.id.rv);

        adapter = new ScanHistoryAdapter(new ScanHistoryAdapter.Listener() {
            @Override
            public void onItemClick(ScanHistoryItem item) {
                generateQuiz(item);
            }

            @Override
            public void onDeleteClick(ScanHistoryItem item) {
                // Disable delete in picker mode
            }
        });
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        registration = scanRepo.userScansQuery().addSnapshotListener((snap, err) -> {
            if (err != null) {
                Toast.makeText(this, "Không tải được lịch sử: " + err.getMessage(),
                        Toast.LENGTH_LONG).show();
                return;
            }
            renderSnapshot(snap);
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private void renderSnapshot(@Nullable QuerySnapshot snap) {
        List<ScanHistoryItem> list = new ArrayList<>();
        if (snap != null) {
            for (DocumentSnapshot doc : snap.getDocuments()) {
                ScanHistoryItem item = doc.toObject(ScanHistoryItem.class);
                if (item != null) {
                    item.setId(doc.getId());
                    list.add(item);
                }
            }
        }
        adapter.submit(list);
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void generateQuiz(ScanHistoryItem item) {
        String uid = quizRepo.currentUid();
        if (uid == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }
        if (item.getSolution() == null || item.getSolution().trim().isEmpty()) {
            Toast.makeText(this, "Bài này chưa có lời giải", Toast.LENGTH_SHORT).show();
            return;
        }

        showQuestionCountDialog(count -> doGenerate(item, uid, count));
    }

    private void showQuestionCountDialog(QuestionCountCallback callback) {
        NumberPicker picker = new NumberPicker(this);
        picker.setMinValue(3);
        picker.setMaxValue(10);
        picker.setValue(5);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        picker.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
                .setTitle("Số câu hỏi")
                .setView(picker)
                .setPositiveButton("Tạo quiz", (d, w) -> callback.onSelected(picker.getValue()))
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void doGenerate(ScanHistoryItem item, String uid, int questionCount) {
        loadingOverlay.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            Quiz quiz;
            try {
                quiz = generator.generate(item.getSolution(), item.getId(), uid, questionCount);
            } catch (Exception e) {
                main.post(() -> {
                    loadingOverlay.setVisibility(View.GONE);
                    showError("Gemini thất bại", e.getMessage());
                });
                return;
            }

            quizRepo.save(quiz)
                    .addOnSuccessListener(saved -> main.post(() -> {
                        loadingOverlay.setVisibility(View.GONE);
                        Toast.makeText(this,
                                "Đã tạo quiz: " + saved.getTitle(),
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }))
                    .addOnFailureListener(e -> main.post(() -> {
                        loadingOverlay.setVisibility(View.GONE);
                        showError("Lưu quiz thất bại", e.getMessage());
                    }));
        });
    }

    private void showError(String title, String message) {
        if (isFinishing() || isDestroyed()) return;
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message != null ? message : "Lỗi không xác định")
                .setPositiveButton("OK", null)
                .show();
    }

    private interface QuestionCountCallback {
        void onSelected(int count);
    }
}
