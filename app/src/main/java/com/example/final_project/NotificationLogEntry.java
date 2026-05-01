package com.example.final_project;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class NotificationLogEntry {

    @Exclude
    private String id;

    private String title;
    private String body;

    @ServerTimestamp
    private Date firedAt;

    public NotificationLogEntry() {}

    @Exclude public String getId() { return id; }
    @Exclude public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public Date getFiredAt() { return firedAt; }
    public void setFiredAt(Date firedAt) { this.firedAt = firedAt; }
}
