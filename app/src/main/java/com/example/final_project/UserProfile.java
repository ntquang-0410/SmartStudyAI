package com.example.final_project;

import com.google.firebase.firestore.Exclude;

public class UserProfile {

    public static final String LEVEL_PRIMARY = "primary";
    public static final String LEVEL_SECONDARY = "secondary";
    public static final String LEVEL_HIGH = "high";
    public static final String LEVEL_UNIVERSITY = "university";
    public static final String LEVEL_OTHER = "other";

    private String displayName;
    private String photoUrl;
    private String birthDate;
    private String school;
    private String eduLevel;
    private Integer grade;

    public UserProfile() {}

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }

    public String getSchool() { return school; }
    public void setSchool(String school) { this.school = school; }

    public String getEduLevel() { return eduLevel; }
    public void setEduLevel(String eduLevel) { this.eduLevel = eduLevel; }

    public Integer getGrade() { return grade; }
    public void setGrade(Integer grade) { this.grade = grade; }

    @Exclude
    public boolean isUniversity() {
        return LEVEL_UNIVERSITY.equals(eduLevel);
    }

    @Exclude
    public boolean usesGrade() {
        return LEVEL_PRIMARY.equals(eduLevel)
                || LEVEL_SECONDARY.equals(eduLevel)
                || LEVEL_HIGH.equals(eduLevel);
    }

    @Exclude
    public int gradeMax() {
        if (LEVEL_PRIMARY.equals(eduLevel)) return 5;
        if (LEVEL_SECONDARY.equals(eduLevel)) return 9;
        if (LEVEL_HIGH.equals(eduLevel)) return 12;
        if (LEVEL_UNIVERSITY.equals(eduLevel)) return 6;
        return 0;
    }

    @Exclude
    public int gradeMin() {
        if (LEVEL_PRIMARY.equals(eduLevel)) return 1;
        if (LEVEL_SECONDARY.equals(eduLevel)) return 6;
        if (LEVEL_HIGH.equals(eduLevel)) return 10;
        if (LEVEL_UNIVERSITY.equals(eduLevel)) return 1;
        return 0;
    }
}
