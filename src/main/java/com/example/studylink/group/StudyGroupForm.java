package com.example.studylink.group;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class StudyGroupForm {

    @NotNull
    private Long courseId;

    @NotBlank
    @Size(max = 120)
    private String name;

    @Size(max = 1200)
    private String description;

    @NotNull
    @Min(2)
    @Max(12)
    private Integer maxMembers = 4;

    @Size(max = 1000)
    private String targetGoals;

    @Size(max = 1000)
    private String preferredTimes;

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getMaxMembers() {
        return maxMembers;
    }

    public void setMaxMembers(Integer maxMembers) {
        this.maxMembers = maxMembers;
    }

    public String getTargetGoals() {
        return targetGoals;
    }

    public void setTargetGoals(String targetGoals) {
        this.targetGoals = targetGoals;
    }

    public String getPreferredTimes() {
        return preferredTimes;
    }

    public void setPreferredTimes(String preferredTimes) {
        this.preferredTimes = preferredTimes;
    }
}
