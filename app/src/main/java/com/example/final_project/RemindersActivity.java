package com.example.final_project;

import android.app.NotificationManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RemindersActivity extends AppCompatActivity {

    private final ReminderRepository repo = new ReminderRepository();
    private ReminderScheduler scheduler;
    private RemindersAdapter adapter;
    private ListenerRegistration registration;

    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reminders);

        scheduler = new ReminderScheduler(this);
        ReminderScheduler.ensureChannel(this);

        View root = findViewById(R.id.root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        TextView btnTest = findViewById(R.id.btn_test);
        btnTest.setOnClickListener(v -> fireTestNotification());

        tvEmpty = findViewById(R.id.tv_empty);
        RecyclerView rv = findViewById(R.id.rv_reminders);
        ExtendedFloatingActionButton fab = findViewById(R.id.fab_add);

        adapter = new RemindersAdapter(new RemindersAdapter.Listener() {
            @Override
            public void onClick(Reminder reminder) {
                showReminderDialog(reminder);
            }

            @Override
            public void onToggle(Reminder reminder, boolean enabled) {
                if (reminder.getId() == null) return;
                repo.setEnabled(reminder.getId(), enabled)
                        .addOnSuccessListener(v -> {
                            reminder.setEnabled(enabled);
                            if (enabled) {
                                long t = scheduler.schedule(reminder);
                                if (t > 0) showNextTriggerToast(t);
                            } else {
                                scheduler.cancel(reminder.getId());
                            }
                        })
                        .addOnFailureListener(e -> Toast.makeText(RemindersActivity.this,
                                "Không cập nhật được: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
            }

            @Override
            public void onDelete(Reminder reminder) {
                confirmDelete(reminder);
            }
        });
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        fab.setOnClickListener(v -> showReminderDialog(null));

        warnIfBlocked();
    }

    @Override
    protected void onStart() {
        super.onStart();
        registration = repo.userRemindersQuery().addSnapshotListener((snap, err) -> {
            if (err != null) {
                Toast.makeText(this, "Không tải được lời nhắc: " + err.getMessage(),
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

    private void renderSnapshot(@Nullable QuerySnapshot snap) {
        List<Reminder> list = new ArrayList<>();
        if (snap != null) {
            for (DocumentSnapshot doc : snap.getDocuments()) {
                Reminder r = doc.toObject(Reminder.class);
                if (r != null) {
                    r.setId(doc.getId());
                    list.add(r);
                }
            }
        }
        Collections.sort(list, new Comparator<Reminder>() {
            @Override
            public int compare(Reminder a, Reminder b) {
                int byHour = Integer.compare(a.getHour(), b.getHour());
                if (byHour != 0) return byHour;
                return Integer.compare(a.getMinute(), b.getMinute());
            }
        });
        adapter.submit(list);
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showReminderDialog(@Nullable Reminder existing) {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_create_reminder, null, false);
        TimePicker timePicker = dialogView.findViewById(R.id.time_picker);
        EditText etLabel = dialogView.findViewById(R.id.et_label);

        timePicker.setIs24HourView(true);

        int initHour;
        int initMinute;
        if (existing != null) {
            initHour = existing.getHour();
            initMinute = existing.getMinute();
            etLabel.setText(existing.getLabel() == null ? "" : existing.getLabel());
        } else {
            Calendar c = Calendar.getInstance();
            initHour = c.get(Calendar.HOUR_OF_DAY);
            initMinute = c.get(Calendar.MINUTE);
        }
        timePicker.setHour(initHour);
        timePicker.setMinute(initMinute);

        new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Thêm lời nhắc" : "Sửa lời nhắc")
                .setView(dialogView)
                .setPositiveButton("Lưu", (d, w) -> {
                    int hour = timePicker.getHour();
                    int minute = timePicker.getMinute();
                    String label = etLabel.getText().toString().trim();
                    saveReminder(existing, hour, minute, label);
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void saveReminder(@Nullable Reminder existing, int hour, int minute, String label) {
        Reminder r;
        if (existing != null) {
            r = existing;
            r.setHour(hour);
            r.setMinute(minute);
            r.setLabel(label);
        } else {
            r = new Reminder(repo.currentUid(), hour, minute, label, true);
        }
        repo.save(r)
                .addOnSuccessListener(saved -> {
                    if (existing != null && existing.getId() != null) {
                        scheduler.cancel(existing.getId());
                    }
                    if (saved.isEnabled()) {
                        long t = scheduler.schedule(saved);
                        if (t > 0) {
                            showNextTriggerToast(t);
                            return;
                        }
                    }
                    Toast.makeText(this, "Đã lưu lời nhắc", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Lưu thất bại: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
    }

    private void confirmDelete(Reminder r) {
        if (r.getId() == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Xoá lời nhắc")
                .setMessage("Xoá lời nhắc " + r.formatTime() + "?")
                .setPositiveButton("Xoá", (d, w) -> {
                    String id = r.getId();
                    repo.delete(id)
                            .addOnSuccessListener(v -> scheduler.cancel(id))
                            .addOnFailureListener(e -> Toast.makeText(this,
                                    "Xoá thất bại: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void fireTestNotification() {
        ReminderScheduler.ensureChannel(this);
        if (!isNotificationsEnabled()) {
            promptOpenNotificationSettings();
            return;
        }
        NotificationCompat.Builder nb = new NotificationCompat.Builder(this, ReminderScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(0xFFF97316)
                .setContentTitle("📚 Test lời nhắc")
                .setContentText("Nếu bạn thấy thông báo này, hệ thống đang hoạt động bình thường.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true);
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) {
            nm.notify(987654, nb.build());
            Toast.makeText(this, "Đã bắn thông báo test", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isNotificationsEnabled() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        return nm != null && nm.areNotificationsEnabled();
    }

    private void promptOpenNotificationSettings() {
        new AlertDialog.Builder(this)
                .setTitle("Thông báo bị tắt")
                .setMessage("Bạn cần bật quyền thông báo cho ứng dụng để nhận lời nhắc.")
                .setPositiveButton("Mở Cài đặt", (d, w) -> {
                    Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                    i.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                    startActivity(i);
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void showNextTriggerToast(long triggerMillis) {
        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
        Toast.makeText(this,
                "Lời nhắc kế tiếp: " + fmt.format(new Date(triggerMillis)),
                Toast.LENGTH_LONG).show();
    }

    private void warnIfBlocked() {
        boolean notif = isNotificationsEnabled();
        boolean exact = scheduler.canScheduleExact();
        if (notif && exact) return;

        StringBuilder msg = new StringBuilder();
        if (!notif) msg.append("• Quyền thông báo đang tắt — sẽ không có notification.\n");
        if (!exact) msg.append("• Quyền hẹn giờ chính xác đang tắt — lời nhắc có thể trễ vài phút.\n");
        msg.append("\nMở cài đặt để cấp quyền?");

        new AlertDialog.Builder(this)
                .setTitle("Cần thêm quyền")
                .setMessage(msg.toString())
                .setPositiveButton("Mở Cài đặt", (d, w) -> {
                    if (!notif) {
                        Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                        i.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                        startActivity(i);
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Intent i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        i.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(i);
                    }
                })
                .setNegativeButton("Bỏ qua", null)
                .show();
    }
}
