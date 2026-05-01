package com.example.final_project;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Quiz {

    @Exclude
    private String id;

    private String uid;
    private String title;
    private String sourceScanId;
    private List<QuizQuestion> questions = new ArrayList<>();

    @ServerTimestamp
    private Date createdAt;

    public Quiz() {}

    public Quiz(String uid, String title, String sourceScanId, List<QuizQuestion> questions) {
        this.uid = uid;
        this.title = title;
        this.sourceScanId = sourceScanId;
        this.questions = questions;
    }

    @Exclude public String getId() { return id; }
    @Exclude public void setId(String id) { this.id = id; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSourceScanId() { return sourceScanId; }
    public void setSourceScanId(String sourceScanId) { this.sourceScanId = sourceScanId; }

    public List<QuizQuestion> getQuestions() { return questions; }
    public void setQuestions(List<QuizQuestion> questions) { this.questions = questions; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
