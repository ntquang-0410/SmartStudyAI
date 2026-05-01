package com.example.final_project;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class ScanHistoryItem {

    @Exclude
    private String id;

    private String uid;
    private String imageUrl;
    private String storagePath;
    private String solution;
    private String preview;
    private String subject;

    @ServerTimestamp
    private Date createdAt;

    public ScanHistoryItem() {}

    public ScanHistoryItem(String uid, String imageUrl, String storagePath,
                           String solution, String preview) {
        this.uid = uid;
        this.imageUrl = imageUrl;
        this.storagePath = storagePath;
        this.solution = solution;
        this.preview = preview;
    }

    @Exclude
    public String getId() { return id; }

    @Exclude
    public void setId(String id) { this.id = id; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

    public String getSolution() { return solution; }
    public void setSolution(String solution) { this.solution = solution; }

    public String getPreview() { return preview; }
    public void setPreview(String preview) { this.preview = preview; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
