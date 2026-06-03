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

    @Test
    void closeAndReopenGroup_ownerUpdatesStatus() {
        Course course = courseRepository.save(course("CS203"));
        User owner = userRepository.save(user("owner4@example.com", "Owner", passwordEncoder));
        StudyGroup group = studyGroupService.createGroup(owner, groupForm(course, "Closable Group"));

        studyGroupService.closeGroup(owner, group.getId());
        assertThat(group.getStatus()).isEqualTo(GroupStatus.CLOSED);

        studyGroupService.reopenGroup(owner, group.getId());
        assertThat(group.getStatus()).isEqualTo(GroupStatus.OPEN);
    }

    @Test
    void deleteGroup_ownerMarksDeletedAndHidesFromLists() {
        Course course = courseRepository.save(course("CS204"));
        User owner = userRepository.save(user("owner5@example.com", "Owner", passwordEncoder));
        User requester = userRepository.save(user("requester5@example.com", "Requester", passwordEncoder));
        StudyGroup group = studyGroupService.createGroup(owner, groupForm(course, "Deleted Group"));
        studyGroupService.requestToJoin(requester, group.getId(), joinRequest("Please."));

        studyGroupService.deleteGroup(owner, group.getId());

        assertThat(group.getStatus()).isEqualTo(GroupStatus.DELETED);
        assertThat(studyGroupService.listGroups()).doesNotContain(group);
        assertThat(studyGroupService.listGroupsForCourse(course)).doesNotContain(group);
        assertThat(studyGroupService.requestsFor(requester)).isEmpty();
    }

    @Test
    void leaveGroup_memberLeavesGroup() {
        Course course = courseRepository.save(course("CS205"));
        User owner = userRepository.save(user("owner6@example.com", "Owner", passwordEncoder));
        User requester = userRepository.save(user("requester6@example.com", "Requester", passwordEncoder));
        StudyGroup group = studyGroupService.createGroup(owner, groupForm(course, "Leave Group"));
        JoinRequest request = studyGroupService.requestToJoin(requester, group.getId(), joinRequest("Please."));
        studyGroupService.approveRequest(owner, request.getId());

        studyGroupService.leaveGroup(requester, group.getId());

        assertThat(studyGroupService.isMember(requester, group)).isFalse();
        assertThat(group.memberCount()).isEqualTo(1);
    }

    @Test
    void leaveGroup_ownerMustTransferOrDeleteFirst() {
        Course course = courseRepository.save(course("CS206"));
        User owner = userRepository.save(user("owner7@example.com", "Owner", passwordEncoder));
        StudyGroup group = studyGroupService.createGroup(owner, groupForm(course, "Owner Leave Group"));

        assertThatThrownBy(() -> studyGroupService.leaveGroup(owner, group.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Transfer ownership");
    }

    @Test
    void removeMember_ownerRemovesOtherMember() {
        Course course = courseRepository.save(course("CS207"));
        User owner = userRepository.save(user("owner8@example.com", "Owner", passwordEncoder));
        User requester = userRepository.save(user("requester8@example.com", "Requester", passwordEncoder));
        StudyGroup group = studyGroupService.createGroup(owner, groupForm(course, "Remove Group"));
        JoinRequest request = studyGroupService.requestToJoin(requester, group.getId(), joinRequest("Please."));
        studyGroupService.approveRequest(owner, request.getId());
        GroupMembership membership = membershipFor(group, requester);

        studyGroupService.removeMember(owner, group.getId(), membership.getId());

        assertThat(studyGroupService.isMember(requester, group)).isFalse();
    }

    @Test
    void transferOwnership_ownerTransfersAdminRole() {
        Course course = courseRepository.save(course("CS208"));
        User owner = userRepository.save(user("owner9@example.com", "Owner", passwordEncoder));
        User requester = userRepository.save(user("requester9@example.com", "Requester", passwordEncoder));
        StudyGroup group = studyGroupService.createGroup(owner, groupForm(course, "Transfer Group"));
        JoinRequest request = studyGroupService.requestToJoin(requester, group.getId(), joinRequest("Please."));
        studyGroupService.approveRequest(owner, request.getId());
        GroupMembership newOwnerMembership = membershipFor(group, requester);

        studyGroupService.transferOwnership(owner, group.getId(), newOwnerMembership.getId());

        assertThat(studyGroupService.isOwner(requester, group)).isTrue();
        assertThat(studyGroupService.isOwner(owner, group)).isFalse();
        assertThat(membershipFor(group, requester).getRole()).isEqualTo(GroupRole.OWNER);
        assertThat(membershipFor(group, owner).getRole()).isEqualTo(GroupRole.MEMBER);
    }

    private GroupMembership membershipFor(StudyGroup group, User user) {
        return studyGroupService.members(group).stream()
                .filter(membership -> membership.getUser().equals(user))
                .findFirst()
                .orElseThrow();
    }
}
