package com.example.studylink.user;

import com.example.studylink.course.Course;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "app_users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String displayName;

    private String department;

    @Column(length = 1000)
    private String skillTags;

    @Column(length = 1000)
    private String availableTimes;

    @Column(length = 1000)
    private String learningGoals;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_courses",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "course_id"))
    private Set<Course> enrolledCourses = new LinkedHashSet<>();

    protected User() {
    }

    public User(String email, String passwordHash, String displayName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDepartment() {
        return department;
    }

    public String getSkillTags() {
        return skillTags;
    }

    public String getAvailableTimes() {
        return availableTimes;
    }

    public String getLearningGoals() {
        return learningGoals;
    }

    public Set<Course> getEnrolledCourses() {
        return enrolledCourses;
    }

    public void updateProfile(ProfileForm form) {
        this.displayName = form.getDisplayName();
        this.department = form.getDepartment();
        this.skillTags = form.getSkillTags();
        this.availableTimes = form.getAvailableTimes();
        this.learningGoals = form.getLearningGoals();
    }

    public void enroll(Course course) {
        enrolledCourses.add(course);
        course.getStudents().add(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User user)) {
            return false;
        }
        return id != null && Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
