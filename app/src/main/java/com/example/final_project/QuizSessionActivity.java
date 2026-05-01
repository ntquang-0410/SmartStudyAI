package com.example.final_project;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class QuizSessionActivity extends AppCompatActivity {

    public static final String EXTRA_QUIZ_ID = "extra_quiz_id";

    private Quiz quiz;
    private int currentIndex = 0;
    private int correctCount = 0;
    private boolean answeredCurrent = false;

    private TextView tvTitle;
    private TextView tvQuestionIndex;
    private TextView tvQuestionContent;
    private LinearProgressIndicator progressQuiz;
    private RadioGroup rgAnswers;
    private RadioButton[] radios;
    private MaterialCardView cardExplanation;
    private TextView tvVerdict;
    private TextView tvExplanation;
    private MaterialCardView cardSummary;
    private TextView tvScore;
    private MaterialButton btnAction;
    private View loadingOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_quiz_session);

        View root = findViewById(R.id.root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        tvTitle = findViewById(R.id.tv_title);
        tvQuestionIndex = findViewById(R.id.tv_question_index);
        tvQuestionContent = findViewById(R.id.tv_question_content);
        progressQuiz = findViewById(R.id.progress_quiz);
        rgAnswers = findViewById(R.id.rg_answers);
        radios = new RadioButton[]{
                findViewById(R.id.rb_a), findViewById(R.id.rb_b),
                findViewById(R.id.rb_c), findViewById(R.id.rb_d)
        };
        cardExplanation = findViewById(R.id.card_explanation);
        tvVerdict = findViewById(R.id.tv_verdict);
        tvExplanation = findViewById(R.id.tv_explanation);
        cardSummary = findViewById(R.id.card_summary);
        tvScore = findViewById(R.id.tv_score);
        btnAction = findViewById(R.id.btn_action);
        loadingOverlay = findViewById(R.id.loading_overlay);

        btnAction.setOnClickListener(v -> onActionClick());

        String quizId = getIntent().getStringExtra(EXTRA_QUIZ_ID);
        if (quizId == null || quizId.isEmpty()) {
            Toast.makeText(this, "Quiz không hợp lệ", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        loadQuiz(quizId);
    }

    private void loadQuiz(String quizId) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() == null
                ? null : FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (uid == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        loadingOverlay.setVisibility(View.VISIBLE);
        FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("quizzes").document(quizId)
                .get()
                .addOnSuccessListener(doc -> {
                    loadingOverlay.setVisibility(View.GONE);
                    quiz = doc.toObject(Quiz.class);
                    if (quiz == null || quiz.getQuestions() == null || quiz.getQuestions().isEmpty()) {
                        Toast.makeText(this, "Quiz trống", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    quiz.setId(doc.getId());
                    tvTitle.setText(quiz.getTitle() != null ? quiz.getTitle() : "Quiz");
                    showQuestion();
                })
                .addOnFailureListener(e -> {
                    loadingOverlay.setVisibility(View.GONE);
                    Toast.makeText(this, "Không tải được quiz: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void showQuestion() {
        List<QuizQuestion> qs = quiz.getQuestions();
        if (currentIndex >= qs.size()) {
            showSummary();
            return;
        }
        QuizQuestion q = qs.get(currentIndex);
        answeredCurrent = false;

        tvQuestionIndex.setText("Câu " + (currentIndex + 1) + "/" + qs.size());
        progressQuiz.setProgress((int) ((currentIndex * 100f) / qs.size()));
        tvQuestionContent.setText(q.getQuestion() != null ? q.getQuestion() : "");

        rgAnswers.clearCheck();
        rgAnswers.setVisibility(View.VISIBLE);
        cardExplanation.setVisibility(View.GONE);
        cardSummary.setVisibility(View.GONE);

        List<String> opts = q.getOptions();
        char prefix = 'A';
        for (int i = 0; i < radios.length; i++) {
            if (opts != null && i < opts.size()) {
                radios[i].setText((char) (prefix + i) + ". " + opts.get(i));
                radios[i].setEnabled(true);
                radios[i].setVisibility(View.VISIBLE);
            } else {
                radios[i].setVisibility(View.GONE);
            }
        }
        btnAction.setText("Xác nhận");
    }

    private void onActionClick() {
        if (quiz == null) return;

        if (cardSummary.getVisibility() == View.VISIBLE) {
            finish();
            return;
        }

        if (!answeredCurrent) {
            int selectedId = rgAnswers.getCheckedRadioButtonId();
            if (selectedId == -1) {
                Toast.makeText(this, "Hãy chọn một đáp án", Toast.LENGTH_SHORT).show();
                return;
            }
            int selectedIndex = selectedRadioIndex(selectedId);
            QuizQuestion q = quiz.getQuestions().get(currentIndex);
            boolean correct = selectedIndex == q.getCorrectIndex();
            if (correct) correctCount++;

            for (RadioButton rb : radios) rb.setEnabled(false);
            answeredCurrent = true;

            tvVerdict.setText(correct ? "✓ Chính xác" : "✗ Chưa đúng");
            tvVerdict.setTextColor(correct ? 0xFF16A34A : 0xFFDC2626);
            String exp = q.getExplanation();
            if (!correct) {
                int ci = q.getCorrectIndex();
                String letter = String.valueOf((char) ('A' + ci));
                exp = "Đáp án đúng: " + letter
                        + (exp == null || exp.isEmpty() ? "" : "\n" + exp);
            }
            tvExplanation.setText(exp != null ? exp : "");
            cardExplanation.setVisibility(View.VISIBLE);

            boolean isLast = currentIndex == quiz.getQuestions().size() - 1;
            btnAction.setText(isLast ? "Xem kết quả" : "Câu tiếp theo");
            return;
        }

        currentIndex++;
        showQuestion();
    }

    private int selectedRadioIndex(int id) {
        for (int i = 0; i < radios.length; i++) {
            if (radios[i].getId() == id) return i;
        }
        return -1;
    }

    private void showSummary() {
        rgAnswers.setVisibility(View.GONE);
        cardExplanation.setVisibility(View.GONE);
        tvQuestionContent.setText("Bạn đã hoàn thành quiz này.");
        tvQuestionIndex.setText("Tổng kết");
        progressQuiz.setProgress(100);

        cardSummary.setVisibility(View.VISIBLE);
        int total = quiz.getQuestions().size();
        tvScore.setText(correctCount + "/" + total);
        btnAction.setText("Hoàn thành");
    }
}
