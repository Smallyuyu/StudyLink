package com.example.studylink.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CourseForm {

    @NotBlank
    @Size(max = 24)
    private String code;

    @NotBlank
    @Size(max = 120)
    private String name;

    @Size(max = 120)
    private String instructor;

    @Size(max = 40)
    private String semester;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInstructor() {
        return instructor;
    }

    public void setInstructor(String instructor) {
        this.instructor = instructor;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }
}
