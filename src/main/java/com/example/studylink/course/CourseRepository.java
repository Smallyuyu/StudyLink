package com.example.studylink.course;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByCode(String code);

    List<Course> findByCodeContainingIgnoreCaseOrNameContainingIgnoreCaseOrderByCode(String code, String name);
}
