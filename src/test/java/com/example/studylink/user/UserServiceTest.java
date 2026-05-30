package com.example.studylink.user;

import static com.example.studylink.TestFixtures.registration;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void register_duplicateEmail_throwsException() {
        userService.register(registration("student@example.com", "Student One"));

        assertThatThrownBy(() -> userService.register(registration("student@example.com", "Student Two")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void register_plainPassword_hashesPassword() {
        User user = userService.register(registration("hash@example.com", "Hash Test"));

        assertThat(user.getPasswordHash()).isNotEqualTo("password");
        assertThat(passwordEncoder.matches("password", user.getPasswordHash())).isTrue();
    }

    @Test
    void updateProfile_validForm_persistsLearnerSignals() {
        User user = userService.register(registration("profile@example.com", "Before"));
        ProfileForm form = new ProfileForm();
        form.setDisplayName("After");
        form.setDepartment("Design");
        form.setSkillTags("research, accessibility");
        form.setAvailableTimes("friday, afternoon");
        form.setLearningGoals("prototype critique");

        User updated = userService.updateProfile(user, form);

        assertThat(updated.getDisplayName()).isEqualTo("After");
        assertThat(updated.getSkillTags()).contains("accessibility");
        assertThat(updated.getLearningGoals()).contains("prototype");
    }
}
