package com.example.final_project;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.Locale;

public class Reminder {

    @Exclude
    private String id;

    private String uid;
    private int hour;
    private int minute;
    private String label;
    private boolean enabled = true;

    @ServerTimestamp
    private Date createdAt;

    public Reminder() {}

    public Reminder(String uid, int hour, int minute, String label, boolean enabled) {
        this.uid = uid;
        this.hour = hour;
        this.minute = minute;
        this.label = label;
        this.enabled = enabled;
    }

    @Exclude public String getId() { return id; }
    @Exclude public void setId(String id) { this.id = id; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public int getHour() { return hour; }
    public void setHour(int hour) { this.hour = hour; }

    public int getMinute() { return minute; }
    public void setMinute(int minute) { this.minute = minute; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    @Exclude
    public String formatTime() {
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
    }
}
