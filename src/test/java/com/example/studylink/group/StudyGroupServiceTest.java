package com.example.studylink.group;

import static com.example.studylink.TestFixtures.course;
import static com.example.studylink.TestFixtures.groupForm;
import static com.example.studylink.TestFixtures.joinRequest;
import static com.example.studylink.TestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.studylink.course.Course;
import com.example.studylink.course.CourseRepository;
import com.example.studylink.user.User;
import com.example.studylink.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StudyGroupServiceTest {

    @Autowired
    private StudyGroupService studyGroupService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void requestToJoin_existingPendingRequest_throwsException() {
        Course course = courseRepository.save(course("CS200"));
        User owner = userRepository.save(user("owner@example.com", "Owner", passwordEncoder));
        User requester = userRepository.save(user("requester@example.com", "Requester", passwordEncoder));
        StudyGroup group = studyGroupService.createGroup(owner, groupForm(course, "Group"));

        studyGroupService.requestToJoin(requester, group.getId(), joinRequest("Let me in."));

        assertThatThrownBy(() -> studyGroupService.requestToJoin(requester, group.getId(), joinRequest("Again.")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pending request");
    }

    @Test
    void approveRequest_nonOwner_throwsAccessDeniedException() {
        Course course = courseRepository.save(course("CS201"));
        User owner = userRepository.save(user("owner2@example.com", "Owner", passwordEncoder));
        User requester = userRepository.save(user("requester2@example.com", "Requester", passwordEncoder));
        User stranger = userRepository.save(user("stranger@example.com", "Stranger", passwordEncoder));
        StudyGroup group = studyGroupService.createGroup(owner, groupForm(course, "Owner Group"));
        JoinRequest request = studyGroupService.requestToJoin(requester, group.getId(), joinRequest("Please."));

        assertThatThrownBy(() -> studyGroupService.approveRequest(stranger, request.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void approveRequest_ownerAddsMember() {
        Course course = courseRepository.save(course("CS202"));
        User owner = userRepository.save(user("owner3@example.com", "Owner", passwordEncoder));
        User requester = userRepository.save(user("requester3@example.com", "Requester", passwordEncoder));
        StudyGroup group = studyGroupService.createGroup(owner, groupForm(course, "Approval Group"));
        JoinRequest request = studyGroupService.requestToJoin(requester, group.getId(), joinRequest("Please."));

        studyGroupService.approveRequest(owner, request.getId());

        assertThat(studyGroupService.isMember(requester, group)).isTrue();
    }
}
