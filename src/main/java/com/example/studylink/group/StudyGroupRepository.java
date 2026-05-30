package com.example.studylink.group;

import com.example.studylink.course.Course;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyGroupRepository extends JpaRepository<StudyGroup, Long> {

    List<StudyGroup> findByCourseOrderByName(Course course);

    List<StudyGroup> findByStatusOrderByName(GroupStatus status);
}
