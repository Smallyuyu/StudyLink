package com.example.studylink.course;

import static com.example.studylink.TestFixtures.course;
import static com.example.studylink.TestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.studylink.user.User;
import com.example.studylink.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CourseServiceTest {

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void joinCourse_existingEnrollment_doesNotDuplicate() {
        User student = userRepository.save(user("course@example.com", "Course Tester", passwordEncoder));
        Course course = courseRepository.save(course("CS100"));

        courseService.enroll(student, course.getId());
        courseService.enroll(student, course.getId());

        User refreshed = userRepository.findById(student.getId()).orElseThrow();
        assertThat(refreshed.getEnrolledCourses()).hasSize(1);
    }

    @Test
    void classmates_enrolledStudents_returnsOtherStudents() {
        User first = userRepository.save(user("first@example.com", "First", passwordEncoder));
        User second = userRepository.save(user("second@example.com", "Second", passwordEncoder));
        Course course = courseRepository.save(course("CS101"));
        courseService.enroll(first, course.getId());
        courseService.enroll(second, course.getId());

        assertThat(courseService.classmates(first, course.getId()))
                .extracting(User::getDisplayName)
                .containsExactly("Second");
    }
}
