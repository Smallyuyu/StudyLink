package com.example.studylink.course;

import com.example.studylink.common.NotFoundException;
import com.example.studylink.user.User;
import com.example.studylink.user.UserRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public CourseService(CourseRepository courseRepository, UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Course> listCourses(String query) {
        if (query == null || query.isBlank()) {
            return courseRepository.findAll().stream()
                    .sorted(Comparator.comparing(Course::getCode))
                    .toList();
        }
        return courseRepository.findByCodeContainingIgnoreCaseOrNameContainingIgnoreCaseOrderByCode(query, query);
    }

    @Transactional(readOnly = true)
    public Course findById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Course not found."));
    }

    @Transactional
    public void enroll(User user, Long courseId) {
        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new NotFoundException("User not found."));
        Course course = findById(courseId);
        managedUser.enroll(course);
    }

    @Transactional(readOnly = true)
    public boolean isEnrolled(User user, Course course) {
        return userRepository.findById(user.getId())
                .map(managed -> managed.getEnrolledCourses().contains(course))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<User> classmates(User user, Long courseId) {
        Course course = findById(courseId);
        return course.getStudents().stream()
                .filter(student -> !student.equals(user))
                .sorted(Comparator.comparing(User::getDisplayName))
                .toList();
    }
}
