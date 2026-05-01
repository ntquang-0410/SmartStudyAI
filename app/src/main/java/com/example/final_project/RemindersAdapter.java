package com.example.final_project;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.List;

public class RemindersAdapter extends RecyclerView.Adapter<RemindersAdapter.VH> {

    public interface Listener {
        void onClick(Reminder reminder);
        void onToggle(Reminder reminder, boolean enabled);
        void onDelete(Reminder reminder);
    }

    private final List<Reminder> items = new ArrayList<>();
    private final Listener listener;

    public RemindersAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Reminder> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reminder, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Reminder r = items.get(position);
        h.tvTime.setText(r.formatTime());
        String label = r.getLabel();
        h.tvLabel.setText(label == null || label.isEmpty() ? "Học bài" : label);

        h.swEnabled.setOnCheckedChangeListener(null);
        h.swEnabled.setChecked(r.isEnabled());
        h.swEnabled.setOnCheckedChangeListener((v, isChecked) -> {
            if (listener != null) listener.onToggle(r, isChecked);
        });

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(r);
        });
        h.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(r);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvTime;
        final TextView tvLabel;
        final MaterialSwitch swEnabled;
        final ImageView btnDelete;

        VH(@NonNull View v) {
            super(v);
            tvTime = v.findViewById(R.id.tv_time);
            tvLabel = v.findViewById(R.id.tv_label);
            swEnabled = v.findViewById(R.id.sw_enabled);
            btnDelete = v.findViewById(R.id.btn_delete);
        }
    }
}
