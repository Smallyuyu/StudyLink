package com.example.studylink.course;

import com.example.studylink.group.StudyGroup;
import com.example.studylink.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    private String instructor;

    private String semester;

    @ManyToMany(mappedBy = "enrolledCourses")
    private Set<User> students = new LinkedHashSet<>();

    @OneToMany(mappedBy = "course")
    private Set<StudyGroup> studyGroups = new LinkedHashSet<>();

    protected Course() {
    }

    public Course(String code, String name, String instructor, String semester) {
        this.code = code;
        this.name = name;
        this.instructor = instructor;
        this.semester = semester;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getInstructor() {
        return instructor;
    }

    public String getSemester() {
        return semester;
    }

    public Set<User> getStudents() {
        return students;
    }

    public Set<StudyGroup> getStudyGroups() {
        return studyGroups;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Course course)) {
            return false;
        }
        return id != null && Objects.equals(id, course.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
