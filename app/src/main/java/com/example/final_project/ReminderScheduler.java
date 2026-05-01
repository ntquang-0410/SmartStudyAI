package com.example.final_project;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

/**
 * Bridges Firestore-stored {@link Reminder}s to local AlarmManager.
 *
 * <p>Strategy: schedule a single {@code setExactAndAllowWhileIdle} alarm for the
 * next occurrence; the receiver fires the notification AND re-arms tomorrow's
 * alarm. This survives Doze and avoids OEM batching that plagues setRepeating.
 *
 * <p>If SCHEDULE_EXACT_ALARM is denied (Android 12+), we fall back to
 * {@code setAndAllowWhileIdle} which is inexact but still wakes idle devices.
 */
public class ReminderScheduler {

    public static final String CHANNEL_ID = "study_reminders";
    public static final String ACTION_FIRE = "com.example.final_project.REMINDER_FIRE";

    public static final String EXTRA_REMINDER_ID = "reminder_id";
    public static final String EXTRA_LABEL = "reminder_label";
    public static final String EXTRA_HOUR = "reminder_hour";
    public static final String EXTRA_MINUTE = "reminder_minute";

    private final Context appContext;

    public ReminderScheduler(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) return;
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return;
        NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID,
                "Nhắc nhở học tập",
                NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Lời nhắc giờ học bài đã đặt");
        ch.enableVibration(true);
        nm.createNotificationChannel(ch);
    }

    /**
     * Schedule next occurrence of this reminder.
     * @return triggering time in millis (so caller can confirm to the user)
     */
    public long schedule(@NonNull Reminder r) {
        if (!r.isEnabled() || r.getId() == null) return 0;
        AlarmManager am = appContext.getSystemService(AlarmManager.class);
        if (am == null) return 0;

        long triggerAt = nextTrigger(r.getHour(), r.getMinute());
        PendingIntent pi = buildPendingIntent(r, PendingIntent.FLAG_UPDATE_CURRENT);

        boolean canExact = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            canExact = am.canScheduleExactAlarms();
        }
        if (canExact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
        return triggerAt;
    }

    /** Re-arm tomorrow's alarm. Called from {@link ReminderReceiver} after firing. */
    public void scheduleNextDay(@NonNull String reminderId, int hour, int minute, String label) {
        Reminder r = new Reminder();
        r.setId(reminderId);
        r.setHour(hour);
        r.setMinute(minute);
        r.setLabel(label);
        r.setEnabled(true);
        schedule(r);
    }

    public void cancel(@NonNull String reminderId) {
        AlarmManager am = appContext.getSystemService(AlarmManager.class);
        if (am == null) return;
        Reminder stub = new Reminder();
        stub.setId(reminderId);
        PendingIntent pi = buildPendingIntent(stub, PendingIntent.FLAG_NO_CREATE);
        if (pi != null) {
            am.cancel(pi);
            pi.cancel();
        }
    }

    /** Pull all reminders from Firestore and reschedule them. Used on app start / boot. */
    public void rescheduleAllFromFirestore(@NonNull Runnable onDone) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() == null
                ? null : FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (uid == null) {
            onDone.run();
            return;
        }
        FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("reminders")
                .get()
                .addOnCompleteListener(task -> {
                    try {
                        if (task.isSuccessful() && task.getResult() != null) {
                            for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                                Reminder r = doc.toObject(Reminder.class);
                                if (r == null) continue;
                                r.setId(doc.getId());
                                if (r.isEnabled()) schedule(r);
                            }
                        }
                    } finally {
                        onDone.run();
                    }
                });
    }

    /** True if the OS will let us schedule exact alarms. Android 12+ may need user grant. */
    public boolean canScheduleExact() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        AlarmManager am = appContext.getSystemService(AlarmManager.class);
        return am != null && am.canScheduleExactAlarms();
    }

    private long nextTrigger(int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        return cal.getTimeInMillis();
    }

    private PendingIntent buildPendingIntent(@NonNull Reminder r, int flags) {
        Intent intent = new Intent(appContext, ReminderReceiver.class);
        intent.setAction(ACTION_FIRE);
        intent.putExtra(EXTRA_REMINDER_ID, r.getId());
        intent.putExtra(EXTRA_LABEL, r.getLabel() != null ? r.getLabel() : "Đến giờ học bài");
        intent.putExtra(EXTRA_HOUR, r.getHour());
        intent.putExtra(EXTRA_MINUTE, r.getMinute());
        intent.setData(android.net.Uri.parse("reminder://" + r.getId()));
        int piFlags = flags | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(appContext, r.getId().hashCode(), intent, piFlags);
    }
}
