package com.example.final_project;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class TrangChuFragment extends Fragment {

    private final ReminderRepository reminderRepo = new ReminderRepository();
    private ListenerRegistration reminderRegistration;
    private ListenerRegistration notifRegistration;

    private TextView tvReminderMain;
    private TextView tvReminderSub;
    private TextView tvStudyHours;
    private TextView tvSessionCount;
    private MaterialCardView cardNextReminder;
    private TextView tvManage;
    private View vBellBadge;
    private ShapeableImageView ivAvatar;
    private TextView tvGreeting;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_trang_chu, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvGreeting = view.findViewById(R.id.tv_greeting);
        ivAvatar = view.findViewById(R.id.iv_avatar);
        cardNextReminder = view.findViewById(R.id.card_next_reminder);
        tvReminderMain = view.findViewById(R.id.tv_reminder_main);
        tvReminderSub = view.findViewById(R.id.tv_reminder_sub);
        tvManage = view.findViewById(R.id.tv_manage_reminders);
        tvStudyHours = view.findViewById(R.id.tv_study_hours);
        tvSessionCount = view.findViewById(R.id.tv_session_count);
        View flBell = view.findViewById(R.id.fl_bell);
        vBellBadge = view.findViewById(R.id.v_bell_badge);

        if (ivAvatar != null) ivAvatar.setOnClickListener(v -> showAccountMenu());

        View.OnClickListener openReminders = v ->
                startActivity(new Intent(requireContext(), RemindersActivity.class));
        if (cardNextReminder != null) cardNextReminder.setOnClickListener(openReminders);
        if (tvManage != null) tvManage.setOnClickListener(openReminders);

        if (flBell != null) {
            flBell.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), NotificationsActivity.class)));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        String uid = reminderRepo.currentUid();
        if (uid == null) return;

        reminderRegistration = reminderRepo.userRemindersQuery()
                .addSnapshotListener((snap, err) -> {
                    if (err != null || !isAdded()) return;
                    renderReminders(snap);
                });

        notifRegistration = FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("notification_log")
                .limit(50)
                .addSnapshotListener((snap, err) -> {
                    if (err != null || !isAdded()) return;
                    refreshBellBadge(snap);
                });

        loadStudyHours(uid);
        loadProfileHeader(uid);
    }

    private void loadProfileHeader(String uid) {
        FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    UserProfile p = doc.exists() ? doc.toObject(UserProfile.class) : null;
                    String name = (p != null && p.getDisplayName() != null && !p.getDisplayName().isEmpty())
                            ? p.getDisplayName()
                            : null;
                    if (name == null) {
                        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
                        if (u != null) {
                            name = u.getDisplayName();
                            if (name == null || name.isEmpty()) name = u.getEmail();
                        }
                    }
                    if (tvGreeting != null) {
                        tvGreeting.setText("Xin chào, " + (name == null ? "" : name) + "! 👋");
                    }
                    if (ivAvatar != null && p != null && p.getPhotoUrl() != null && !p.getPhotoUrl().isEmpty()) {
                        ImageLoader.load(ivAvatar, p.getPhotoUrl(), android.R.drawable.sym_def_app_icon);
                    }
                });
    }

    @Override
    public void onStop() {
        super.onStop();
        if (reminderRegistration != null) {
            reminderRegistration.remove();
            reminderRegistration = null;
        }
        if (notifRegistration != null) {
            notifRegistration.remove();
            notifRegistration = null;
        }
    }

    private void refreshBellBadge(@Nullable QuerySnapshot snap) {
        if (vBellBadge == null) return;
        long lastSeen = requireContext()
                .getSharedPreferences(NotificationsActivity.PREFS, 0)
                .getLong(NotificationsActivity.KEY_LAST_SEEN, 0L);
        boolean hasUnread = false;
        if (snap != null) {
            for (DocumentSnapshot d : snap.getDocuments()) {
                NotificationLogEntry e = d.toObject(NotificationLogEntry.class);
                if (e == null) continue;
                Date fired = e.getFiredAt();
                if (fired != null && fired.getTime() > lastSeen) {
                    hasUnread = true;
                    break;
                }
            }
        }
        vBellBadge.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
    }

    private void loadStudyHours(String uid) {
        FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    long studySeconds = 0;
                    long sessionCount = 0;
                    if (doc.exists()) {
                        Long s = doc.getLong("studySeconds");
                        Long c = doc.getLong("sessionCount");
                        if (s != null) studySeconds = s;
                        if (c != null) sessionCount = c;
                    }
                    long hours = studySeconds / 3600;
                    long mins = (studySeconds % 3600) / 60;
                    if (tvStudyHours != null) {
                        tvStudyHours.setText(hours + " giờ " + mins + " phút");
                    }
                    if (tvSessionCount != null) {
                        tvSessionCount.setText(String.valueOf(sessionCount));
                    }
                });
    }

    private void renderReminders(@Nullable QuerySnapshot snap) {
        List<Reminder> active = new ArrayList<>();
        if (snap != null) {
            for (DocumentSnapshot doc : snap.getDocuments()) {
                Reminder r = doc.toObject(Reminder.class);
                if (r != null && r.isEnabled()) {
                    r.setId(doc.getId());
                    active.add(r);
                }
            }
        }
        if (active.isEmpty()) {
            tvReminderMain.setText("Chưa có lời nhắc");
            tvReminderSub.setText("Chạm để tạo lịch học");
            return;
        }
        Reminder next = findNext(active);
        tvReminderMain.setText(next.formatTime());
        String label = next.getLabel();
        String tail = (active.size() > 1)
                ? " • Tổng " + active.size() + " lời nhắc"
                : "";
        tvReminderSub.setText((label == null || label.isEmpty() ? "Học bài" : label) + tail);
    }

    private Reminder findNext(@NonNull List<Reminder> reminders) {
        Calendar now = Calendar.getInstance();
        int nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        Reminder bestUpcoming = null;
        Reminder earliest = null;
        for (Reminder r : reminders) {
            int rMinutes = r.getHour() * 60 + r.getMinute();
            if (earliest == null
                    || rMinutes < earliest.getHour() * 60 + earliest.getMinute()) {
                earliest = r;
            }
            if (rMinutes >= nowMinutes) {
                if (bestUpcoming == null
                        || rMinutes < bestUpcoming.getHour() * 60 + bestUpcoming.getMinute()) {
                    bestUpcoming = r;
                }
            }
        }
        return bestUpcoming != null ? bestUpcoming : earliest;
    }

    private void showAccountMenu() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String email = user != null ? user.getEmail() : "";

        new AlertDialog.Builder(requireContext())
                .setTitle("Tài khoản")
                .setMessage(email)
                .setPositiveButton("Chỉnh sửa hồ sơ",
                        (d, w) -> startActivity(new Intent(requireContext(), ProfileActivity.class)))
                .setNeutralButton("Đăng xuất", (d, w) -> logout())
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
