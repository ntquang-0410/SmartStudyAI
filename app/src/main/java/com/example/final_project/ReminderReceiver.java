package com.example.final_project;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ReminderScheduler.ACTION_FIRE.equals(intent.getAction())) return;

        ReminderScheduler.ensureChannel(context);

        String id = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_ID);
        String label = intent.getStringExtra(ReminderScheduler.EXTRA_LABEL);
        int hour = intent.getIntExtra(ReminderScheduler.EXTRA_HOUR, -1);
        int minute = intent.getIntExtra(ReminderScheduler.EXTRA_MINUTE, -1);
        if (label == null) label = "Đến giờ học bài";
        String time = (hour >= 0 && minute >= 0)
                ? String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                : "";

        Intent open = new Intent(context, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentPi = PendingIntent.getActivity(
                context,
                id == null ? 0 : id.hashCode(),
                open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(0xFFF97316)
                .setContentTitle("📚 " + label)
                .setContentText(time.isEmpty() ? "Đến giờ học bài rồi!" : ("Bây giờ là " + time + " — đến giờ học!"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(contentPi);

        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm != null) {
            int notifId = id == null ? 1 : Math.abs(id.hashCode());
            nm.notify(notifId, nb.build());
        }

        logNotification(label, time);

        // Re-arm next day so this reminder keeps firing daily.
        if (id != null && hour >= 0 && minute >= 0) {
            new ReminderScheduler(context).scheduleNextDay(id, hour, minute, label);
        }
    }

    private void logNotification(String label, String time) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String body = time.isEmpty()
                ? "Đến giờ học bài rồi!"
                : "Bây giờ là " + time + " — đến giờ học!";

        Map<String, Object> entry = new HashMap<>();
        entry.put("title", "📚 " + label);
        entry.put("body", body);
        entry.put("firedAt", FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid())
                .collection("notification_log")
                .add(entry);
    }
}
