package com.example.final_project;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class LichSuFragment extends Fragment {

    private final ScanHistoryRepository repo = new ScanHistoryRepository();
    private ScanHistoryAdapter adapter;
    private ListenerRegistration registration;

    private RecyclerView rv;
    private ProgressBar progress;
    private TextView tvEmpty;
    private ChipGroup cgFilter;

    private final List<ScanHistoryItem> allItems = new ArrayList<>();
    @Nullable private String activeSubject;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lich_su, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rv = view.findViewById(R.id.rv_history);
        progress = view.findViewById(R.id.progress);
        tvEmpty = view.findViewById(R.id.tv_empty);
        cgFilter = view.findViewById(R.id.cg_filter);
        buildFilterChips();

        adapter = new ScanHistoryAdapter(new ScanHistoryAdapter.Listener() {
            @Override
            public void onItemClick(ScanHistoryItem item) {
                openDetail(item);
            }

            @Override
            public void onDeleteClick(ScanHistoryItem item) {
                confirmDelete(item);
            }
        });

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);
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
        registration = repo.userScansQuery().addSnapshotListener((snap, err) -> {
            progress.setVisibility(View.GONE);
            if (err != null) {
                Toast.makeText(requireContext(),
                        "Không tải được lịch sử: " + err.getMessage(),
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
        allItems.clear();
        if (snap != null) {
            for (DocumentSnapshot doc : snap.getDocuments()) {
                ScanHistoryItem item = doc.toObject(ScanHistoryItem.class);
                if (item != null) {
                    item.setId(doc.getId());
                    allItems.add(item);
                }
            }
        }
        applyFilter();
    }

    private void buildFilterChips() {
        if (cgFilter == null) return;
        cgFilter.removeAllViews();

        Chip all = new Chip(requireContext());
        all.setText("Tất cả");
        all.setCheckable(true);
        all.setChecked(true);
        all.setTag(null);
        cgFilter.addView(all);

        for (String subject : SubjectClassifier.ALL) {
            Chip c = new Chip(requireContext());
            c.setText(subject);
            c.setCheckable(true);
            c.setTag(subject);
            cgFilter.addView(c);
        }

        cgFilter.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) {
                // Re-check the previously selected chip — singleSelection allows none otherwise.
                int defaultId = all.getId();
                cgFilter.check(defaultId);
                return;
            }
            View checked = cgFilter.findViewById(ids.get(0));
            activeSubject = checked == null ? null : (String) checked.getTag();
            applyFilter();
        });
    }

    private void applyFilter() {
        List<ScanHistoryItem> filtered;
        if (activeSubject == null) {
            filtered = new ArrayList<>(allItems);
        } else {
            filtered = new ArrayList<>();
            for (ScanHistoryItem it : allItems) {
                if (activeSubject.equals(it.getSubject())) filtered.add(it);
            }
        }
        adapter.submit(filtered);
        tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        tvEmpty.setText(allItems.isEmpty()
                ? "Chưa có bài nào trong lịch sử.\nHãy quét một bài để bắt đầu!"
                : "Không có bài nào thuộc môn này.");
    }

    private void openDetail(ScanHistoryItem item) {
        Intent intent = new Intent(requireContext(), HistoryDetailActivity.class);
        intent.putExtra(HistoryDetailActivity.EXTRA_IMAGE_URL, item.getImageUrl());
        intent.putExtra(HistoryDetailActivity.EXTRA_SOLUTION, item.getSolution());
        intent.putExtra(HistoryDetailActivity.EXTRA_DATE,
                item.getCreatedAt() == null ? 0L : item.getCreatedAt().getTime());
        startActivity(intent);
    }

    private void confirmDelete(ScanHistoryItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xoá khỏi lịch sử")
                .setMessage("Bạn chắc chắn muốn xoá bài này?")
                .setPositiveButton("Xoá", (d, w) -> repo.delete(item)
                        .addOnFailureListener(e -> Toast.makeText(requireContext(),
                                "Xoá thất bại: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()))
                .setNegativeButton("Huỷ", null)
                .show();
    }
}
