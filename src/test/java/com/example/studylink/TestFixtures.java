package com.example.studylink;

import com.example.studylink.course.Course;
import com.example.studylink.group.JoinRequestForm;
import com.example.studylink.group.StudyGroupForm;
import com.example.studylink.user.ProfileForm;
import com.example.studylink.user.RegistrationForm;
import com.example.studylink.user.User;
import org.springframework.security.crypto.password.PasswordEncoder;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static RegistrationForm registration(String email, String displayName) {
        RegistrationForm form = new RegistrationForm();
        form.setEmail(email);
        form.setDisplayName(displayName);
        form.setPassword("password");
        return form;
    }

    public static User user(String email, String displayName, PasswordEncoder passwordEncoder) {
        User user = new User(email, passwordEncoder.encode("password"), displayName);
        ProfileForm form = new ProfileForm();
        form.setDisplayName(displayName);
        form.setDepartment("Computer Science");
        form.setSkillTags("python, java, sql");
        form.setAvailableTimes("monday, night");
        form.setLearningGoals("machine learning, backend systems");
        user.updateProfile(form);
        return user;
    }

    public static Course course(String code) {
        return new Course(code, "Test Course " + code, "Dr. Test", "Spring 2026");
    }

    public static StudyGroupForm groupForm(Course course, String name) {
        StudyGroupForm form = new StudyGroupForm();
        form.setCourseId(course.getId());
        form.setName(name);
        form.setDescription("Practice together with focused weekly goals.");
        form.setMaxMembers(4);
        form.setTargetGoals("python, machine learning, backend systems");
        form.setPreferredTimes("monday, night");
        return form;
    }

    public static JoinRequestForm joinRequest(String message) {
        JoinRequestForm form = new JoinRequestForm();
        form.setMessage(message);
        return form;
    }
}
