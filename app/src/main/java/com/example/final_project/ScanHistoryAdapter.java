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

public class ScanHistoryAdapter extends RecyclerView.Adapter<ScanHistoryAdapter.VH> {

    public interface Listener {
        void onItemClick(ScanHistoryItem item);
        void onDeleteClick(ScanHistoryItem item);
    }

    private final List<ScanHistoryItem> items = new ArrayList<>();
    private final Listener listener;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public ScanHistoryAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<ScanHistoryItem> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_scan_history, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ScanHistoryItem item = items.get(position);

        Date created = item.getCreatedAt();
        h.tvDate.setText(created != null ? dateFormat.format(created) : "Đang lưu…");

        String preview = item.getPreview();
        if (preview == null || preview.isEmpty()) {
            preview = item.getSolution();
        }
        h.tvPreview.setText(preview != null ? preview : "");

        ImageLoader.load(h.ivThumb, item.getImageUrl(), R.drawable.bg_image_placeholder);

        SubjectChipUi.apply(h.tvSubject, item.getSubject());

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
        h.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView ivThumb;
        final TextView tvDate;
        final TextView tvPreview;
        final TextView tvSubject;
        final ImageView btnDelete;

        VH(@NonNull View v) {
            super(v);
            ivThumb = v.findViewById(R.id.iv_thumb);
            tvDate = v.findViewById(R.id.tv_date);
            tvPreview = v.findViewById(R.id.tv_preview);
            tvSubject = v.findViewById(R.id.tv_subject);
            btnDelete = v.findViewById(R.id.btn_delete);
        }
    }
}
