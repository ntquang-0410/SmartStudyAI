package com.example.final_project;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class QuizFragment extends Fragment {

    private final QuizRepository repo = new QuizRepository();
    private QuizListAdapter adapter;
    private ListenerRegistration registration;

    private RecyclerView rv;
    private ProgressBar progress;
    private TextView tvEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quiz, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rv = view.findViewById(R.id.rv_quizzes);
        progress = view.findViewById(R.id.progress);
        tvEmpty = view.findViewById(R.id.tv_empty);
        ExtendedFloatingActionButton fab = view.findViewById(R.id.fab_create);

        adapter = new QuizListAdapter(new QuizListAdapter.Listener() {
            @Override
            public void onClick(Quiz quiz) {
                openQuiz(quiz);
            }

            @Override
            public void onDelete(Quiz quiz) {
                confirmDelete(quiz);
            }
        });
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        fab.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), ScanPickerActivity.class));
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (repo.currentUid() == null) {
            tvEmpty.setText("Bạn chưa đăng nhập");
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }
        progress.setVisibility(View.VISIBLE);
        registration = repo.userQuizzesQuery().addSnapshotListener((snap, err) -> {
            progress.setVisibility(View.GONE);
            if (err != null) {
                Toast.makeText(requireContext(),
                        "Không tải được quiz: " + err.getMessage(),
                        Toast.LENGTH_LONG).show();
                return;
            }
            renderSnapshot(snap);
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    private void renderSnapshot(@Nullable QuerySnapshot snap) {
        List<Quiz> list = new ArrayList<>();
        if (snap != null) {
            for (DocumentSnapshot doc : snap.getDocuments()) {
                Quiz q = doc.toObject(Quiz.class);
                if (q != null) {
                    q.setId(doc.getId());
                    list.add(q);
                }
            }
        }
        adapter.submit(list);
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openQuiz(Quiz quiz) {
        Intent intent = new Intent(requireContext(), QuizSessionActivity.class);
        intent.putExtra(QuizSessionActivity.EXTRA_QUIZ_ID, quiz.getId());
        startActivity(intent);
    }

    private void confirmDelete(Quiz quiz) {
        if (quiz.getId() == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Xoá quiz")
                .setMessage("Xoá quiz \"" + (quiz.getTitle() == null ? "" : quiz.getTitle()) + "\"?")
                .setPositiveButton("Xoá", (d, w) -> repo.delete(quiz.getId())
                        .addOnFailureListener(e -> Toast.makeText(requireContext(),
                                "Xoá thất bại: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()))
                .setNegativeButton("Huỷ", null)
                .show();
    }
}
