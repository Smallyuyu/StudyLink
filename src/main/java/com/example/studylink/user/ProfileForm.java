package com.example.studylink.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ProfileForm {

    @NotBlank
    @Size(max = 80)
    private String displayName;

    @Size(max = 120)
    private String department;

    @Size(max = 1000)
    private String skillTags;

    @Size(max = 1000)
    private String availableTimes;

    @Size(max = 1000)
    private String learningGoals;

    public static ProfileForm from(User user) {
        ProfileForm form = new ProfileForm();
        form.setDisplayName(user.getDisplayName());
        form.setDepartment(user.getDepartment());
        form.setSkillTags(user.getSkillTags());
        form.setAvailableTimes(user.getAvailableTimes());
        form.setLearningGoals(user.getLearningGoals());
        return form;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getSkillTags() {
        return skillTags;
    }

    public void setSkillTags(String skillTags) {
        this.skillTags = skillTags;
    }

    public String getAvailableTimes() {
        return availableTimes;
    }

    public void setAvailableTimes(String availableTimes) {
        this.availableTimes = availableTimes;
    }

    public String getLearningGoals() {
        return learningGoals;
    }

    public void setLearningGoals(String learningGoals) {
        this.learningGoals = learningGoals;
    }
}
