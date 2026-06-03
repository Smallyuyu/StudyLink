package com.example.studylink.group;

import com.example.studylink.common.NotFoundException;
import com.example.studylink.course.Course;
import com.example.studylink.course.CourseRepository;
import com.example.studylink.user.User;
import com.example.studylink.user.UserRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudyGroupService {

    private final StudyGroupRepository studyGroupRepository;
    private final GroupMembershipRepository membershipRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public StudyGroupService(StudyGroupRepository studyGroupRepository,
            GroupMembershipRepository membershipRepository,
            JoinRequestRepository joinRequestRepository,
            CourseRepository courseRepository,
            UserRepository userRepository) {
        this.studyGroupRepository = studyGroupRepository;
        this.membershipRepository = membershipRepository;
        this.joinRequestRepository = joinRequestRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<StudyGroup> listGroups() {
        return studyGroupRepository.findAll().stream()
                .filter(group -> !group.isDeleted())
                .sorted(Comparator.comparing(group -> group.getCourse().getCode() + group.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StudyGroup> listGroupsForCourse(Course course) {
        return studyGroupRepository.findByCourseOrderByName(course).stream()
                .filter(group -> !group.isDeleted())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StudyGroup> openGroups() {
        return studyGroupRepository.findByStatusOrderByName(GroupStatus.OPEN);
    }

    @Transactional(readOnly = true)
    public StudyGroup findById(Long id) {
        return studyGroupRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Study group not found."));
    }

    @Transactional
    public StudyGroup createGroup(User owner, StudyGroupForm form) {
        User managedOwner = managedUser(owner);
        Course course = courseRepository.findById(form.getCourseId())
                .orElseThrow(() -> new NotFoundException("Course not found."));
        if (!managedOwner.getEnrolledCourses().contains(course)) {
            managedOwner.enroll(course);
        }

        StudyGroup group = new StudyGroup(
                form.getName().trim(),
                form.getDescription(),
                form.getMaxMembers(),
                form.getTargetGoals(),
                form.getPreferredTimes(),
                course,
                managedOwner);
        studyGroupRepository.save(group);
        GroupMembership ownerMembership = new GroupMembership(group, managedOwner, GroupRole.OWNER);
        group.getMemberships().add(ownerMembership);
        membershipRepository.save(ownerMembership);
        return group;
    }

    @Transactional
    public JoinRequest requestToJoin(User requester, Long groupId, JoinRequestForm form) {
        User managedRequester = managedUser(requester);
        StudyGroup group = findById(groupId);
        ensureNotDeleted(group);
        if (!group.hasOpenSlot()) {
            throw new IllegalStateException("This group is not accepting new members.");
        }
        if (membershipRepository.existsByStudyGroupAndUser(group, managedRequester)) {
            throw new IllegalStateException("You are already a member of this group.");
        }
        if (joinRequestRepository.existsByStudyGroupAndRequesterAndStatus(group, managedRequester, RequestStatus.PENDING)) {
            throw new IllegalStateException("You already have a pending request for this group.");
        }
        JoinRequest request = new JoinRequest(group, managedRequester, normalizedJoinMessage(form));
        return joinRequestRepository.save(request);
    }

    @Transactional
    public JoinRequest approveRequest(User owner, Long requestId) {
        JoinRequest request = findRequest(requestId);
        StudyGroup group = request.getStudyGroup();
        ensureOwner(owner, group);
        ensurePending(request);
        if (!group.hasOpenSlot()) {
            throw new IllegalStateException("This group is full or closed.");
        }
        if (!membershipRepository.existsByStudyGroupAndUser(group, request.getRequester())) {
            GroupMembership membership = new GroupMembership(group, request.getRequester(), GroupRole.MEMBER);
            group.getMemberships().add(membership);
            membershipRepository.save(membership);
        }
        request.approve();
        group.markFullIfNeeded();
        return request;
    }

    @Transactional
    public JoinRequest declineRequest(User owner, Long requestId) {
        JoinRequest request = findRequest(requestId);
        ensureOwner(owner, request.getStudyGroup());
        ensurePending(request);
        request.decline();
        return request;
    }

    @Transactional
    public void closeGroup(User owner, Long groupId) {
        StudyGroup group = findById(groupId);
        ensureOwner(owner, group);
        ensureNotDeleted(group);
        group.setStatus(GroupStatus.CLOSED);
    }

    @Transactional
    public void reopenGroup(User owner, Long groupId) {
        StudyGroup group = findById(groupId);
        ensureOwner(owner, group);
        ensureNotDeleted(group);
        if (group.memberCount() >= group.getMaxMembers()) {
            group.setStatus(GroupStatus.FULL);
        } else {
            group.setStatus(GroupStatus.OPEN);
        }
    }

    @Transactional
    public void deleteGroup(User owner, Long groupId) {
        StudyGroup group = findById(groupId);
        ensureOwner(owner, group);
        group.setStatus(GroupStatus.DELETED);
        pendingRequests(group).forEach(JoinRequest::decline);
    }

    @Transactional
    public void leaveGroup(User user, Long groupId) {
        User managedUser = managedUser(user);
        StudyGroup group = findById(groupId);
        ensureNotDeleted(group);
        if (isOwner(managedUser, group)) {
            throw new IllegalStateException("Transfer ownership or delete the group before leaving.");
        }
        GroupMembership membership = membershipRepository.findByStudyGroupAndUser(group, managedUser)
                .orElseThrow(() -> new IllegalStateException("You are not a member of this group."));
        removeMembership(group, membership);
    }

    @Transactional
    public void removeMember(User owner, Long groupId, Long membershipId) {
        StudyGroup group = findById(groupId);
        ensureOwner(owner, group);
        ensureNotDeleted(group);
        GroupMembership membership = findMembership(membershipId);
        ensureMembershipInGroup(membership, group);
        if (Objects.equals(membership.getUser().getId(), group.getOwner().getId())) {
            throw new IllegalStateException("The group owner cannot be removed.");
        }
        removeMembership(group, membership);
    }

    @Transactional
    public void transferOwnership(User owner, Long groupId, Long membershipId) {
        User managedOwner = managedUser(owner);
        StudyGroup group = findById(groupId);
        if (!isOwner(managedOwner, group)) {
            throw new AccessDeniedException("Only the group owner can perform this action.");
        }
        ensureNotDeleted(group);

        GroupMembership newOwnerMembership = findMembership(membershipId);
        ensureMembershipInGroup(newOwnerMembership, group);
        if (Objects.equals(newOwnerMembership.getUser().getId(), managedOwner.getId())) {
            throw new IllegalStateException("Choose another member to become the group owner.");
        }

        GroupMembership currentOwnerMembership = membershipRepository.findByStudyGroupAndUser(group, managedOwner)
                .orElseThrow(() -> new IllegalStateException("Current owner membership not found."));
        currentOwnerMembership.setRole(GroupRole.MEMBER);
        newOwnerMembership.setRole(GroupRole.OWNER);
        group.transferOwnership(newOwnerMembership.getUser());
    }

    @Transactional(readOnly = true)
    public List<GroupMembership> members(StudyGroup group) {
        return membershipRepository.findByStudyGroupOrderByJoinedAt(group);
    }

    @Transactional(readOnly = true)
    public List<GroupMembership> membershipsFor(User user) {
        return membershipRepository.findByUserOrderByJoinedAtDesc(user).stream()
                .filter(membership -> !membership.getStudyGroup().isDeleted())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<JoinRequest> pendingRequests(StudyGroup group) {
        return joinRequestRepository.findByStudyGroupAndStatusOrderByCreatedAtAsc(group, RequestStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<JoinRequest> requestsFor(User user) {
        return joinRequestRepository.findByRequesterOrderByCreatedAtDesc(user).stream()
                .filter(request -> !request.getStudyGroup().isDeleted())
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isMember(User user, StudyGroup group) {
        return membershipRepository.existsByStudyGroupAndUser(group, user);
    }

    @Transactional(readOnly = true)
    public boolean isOwner(User user, StudyGroup group) {
        return user != null && Objects.equals(group.getOwner().getId(), user.getId());
    }

    @Transactional(readOnly = true)
    public GroupMembership requireActiveMembership(User user, Long groupId) {
        User managedUser = managedUser(user);
        StudyGroup group = findById(groupId);
        ensureNotDeleted(group);
        return membershipRepository.findByStudyGroupAndUser(group, managedUser)
                .orElseThrow(() -> new AccessDeniedException("Only group members can use this chat."));
    }

    private JoinRequest findRequest(Long requestId) {
        return joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Join request not found."));
    }

    private GroupMembership findMembership(Long membershipId) {
        return membershipRepository.findById(membershipId)
                .orElseThrow(() -> new NotFoundException("Group membership not found."));
    }

    private User managedUser(User user) {
        return userRepository.findById(user.getId())
                .orElseThrow(() -> new NotFoundException("User not found."));
    }

    private void ensureOwner(User user, StudyGroup group) {
        User managedUser = managedUser(user);
        if (!isOwner(managedUser, group)) {
            throw new AccessDeniedException("Only the group owner can perform this action.");
        }
    }

    private void ensureNotDeleted(StudyGroup group) {
        if (group.isDeleted()) {
            throw new IllegalStateException("This group has been deleted.");
        }
    }

    private void ensureMembershipInGroup(GroupMembership membership, StudyGroup group) {
        if (!Objects.equals(membership.getStudyGroup().getId(), group.getId())) {
            throw new NotFoundException("Group membership not found.");
        }
    }

    private void removeMembership(StudyGroup group, GroupMembership membership) {
        group.getMemberships().remove(membership);
        membershipRepository.delete(membership);
        group.markOpenIfSeatAvailable();
    }

    private void ensurePending(JoinRequest request) {
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("This request has already been reviewed.");
        }
    }

    private String normalizedJoinMessage(JoinRequestForm form) {
        String message = form.getMessage();
        if (message == null) {
            return null;
        }

        String normalizedMessage = message.trim();
        if (normalizedMessage.length() > JoinRequestForm.MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Join request message is too long.");
        }
        return normalizedMessage.isEmpty() ? null : normalizedMessage;
    }
}
