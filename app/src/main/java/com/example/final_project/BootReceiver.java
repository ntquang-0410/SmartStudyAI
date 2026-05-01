package com.example.final_project;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !"android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            return;
        }
        ReminderScheduler.ensureChannel(context);
        PendingResult pr = goAsync();
        new ReminderScheduler(context).rescheduleAllFromFirestore(pr::finish);
    }
}
