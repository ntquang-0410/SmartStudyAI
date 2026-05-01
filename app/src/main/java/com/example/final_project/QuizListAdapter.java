package com.example.final_project;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class QuizListAdapter extends RecyclerView.Adapter<QuizListAdapter.VH> {

    public interface Listener {
        void onClick(Quiz quiz);
        void onDelete(Quiz quiz);
    }

    private final List<Quiz> items = new ArrayList<>();
    private final Listener listener;
    private final SimpleDateFormat fmt =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public QuizListAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Quiz> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_quiz, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Quiz q = items.get(position);
        h.title.setText(q.getTitle() != null ? q.getTitle() : "Quiz");

        Date created = q.getCreatedAt();
        int n = q.getQuestions() == null ? 0 : q.getQuestions().size();
        String meta = n + " câu • " + (created == null ? "Vừa tạo" : fmt.format(created));
        h.meta.setText(meta);

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(q);
        });
        h.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(q);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView meta;
        final ImageView btnDelete;

        VH(@NonNull View v) {
            super(v);
            title = v.findViewById(R.id.tv_quiz_title);
            meta = v.findViewById(R.id.tv_quiz_meta);
            btnDelete = v.findViewById(R.id.btn_delete_quiz);
        }
    }
}
