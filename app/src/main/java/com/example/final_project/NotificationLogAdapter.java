package com.example.final_project;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class NotificationLogAdapter extends RecyclerView.Adapter<NotificationLogAdapter.VH> {

    private final List<NotificationLogEntry> items = new ArrayList<>();

    public void submit(List<NotificationLogEntry> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification_log, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        NotificationLogEntry e = items.get(position);
        h.tvTitle.setText(e.getTitle() != null ? e.getTitle() : "");
        h.tvBody.setText(e.getBody() != null ? e.getBody() : "");
        if (e.getFiredAt() != null) {
            CharSequence rel = DateUtils.getRelativeTimeSpanString(
                    e.getFiredAt().getTime(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE);
            h.tvTime.setText(rel);
            h.tvTime.setVisibility(View.VISIBLE);
        } else {
            h.tvTime.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvTitle, tvBody, tvTime;
        VH(@NonNull View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tv_notif_title);
            tvBody  = v.findViewById(R.id.tv_notif_body);
            tvTime  = v.findViewById(R.id.tv_notif_time);
        }
    }
}
