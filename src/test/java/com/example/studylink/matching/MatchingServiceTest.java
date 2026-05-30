package com.example.studylink.matching;

import static com.example.studylink.TestFixtures.course;
import static com.example.studylink.TestFixtures.groupForm;
import static com.example.studylink.TestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.studylink.course.Course;
import com.example.studylink.course.CourseRepository;
import com.example.studylink.course.CourseService;
import com.example.studylink.group.StudyGroup;
import com.example.studylink.group.StudyGroupForm;
import com.example.studylink.group.StudyGroupService;
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
class MatchingServiceTest {

    @Autowired
    private MatchingService matchingService;

    @Autowired
    private StudyGroupService studyGroupService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void recommendGroups_matchingTags_returnsSortedResults() {
        Course sameCourse = courseRepository.save(course("CS300"));
        Course otherCourse = courseRepository.save(course("UX300"));
        User learner = userRepository.save(user("learner@example.com", "Learner", passwordEncoder));
        User owner = userRepository.save(user("match-owner@example.com", "Owner", passwordEncoder));
        User otherOwner = userRepository.save(user("other-owner@example.com", "Other Owner", passwordEncoder));
        courseService.enroll(learner, sameCourse.getId());

        StudyGroup strongMatch = studyGroupService.createGroup(owner, groupForm(sameCourse, "Strong Match"));
        StudyGroupForm weakForm = groupForm(otherCourse, "Weak Match");
        weakForm.setTargetGoals("watercolor sketching");
        weakForm.setPreferredTimes("sunday morning");
        StudyGroup weakMatch = studyGroupService.createGroup(otherOwner, weakForm);

        assertThat(matchingService.recommendGroups(learner))
                .extracting(result -> result.group().getName())
                .containsSubsequence(strongMatch.getName(), weakMatch.getName());
        assertThat(matchingService.recommendGroups(learner).getFirst().reasons())
                .anyMatch(reason -> reason.contains("same course"));
    }
}
