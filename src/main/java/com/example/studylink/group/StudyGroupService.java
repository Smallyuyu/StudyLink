package com.example.studylink.group;

import com.example.studylink.common.NotFoundException;
import com.example.studylink.course.Course;
import com.example.studylink.course.CourseRepository;
import com.example.studylink.user.User;
import com.example.studylink.user.UserRepository;
import java.util.Comparator;
import java.util.List;
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
                .sorted(Comparator.comparing(group -> group.getCourse().getCode() + group.getName()))
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
        if (!group.hasOpenSlot()) {
            throw new IllegalStateException("This group is not accepting new members.");
        }
        if (membershipRepository.existsByStudyGroupAndUser(group, managedRequester)) {
            throw new IllegalStateException("You are already a member of this group.");
        }
        if (joinRequestRepository.existsByStudyGroupAndRequesterAndStatus(group, managedRequester, RequestStatus.PENDING)) {
            throw new IllegalStateException("You already have a pending request for this group.");
        }
        JoinRequest request = new JoinRequest(group, managedRequester, form.getMessage());
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
        group.setStatus(GroupStatus.CLOSED);
    }

    @Transactional
    public void reopenGroup(User owner, Long groupId) {
        StudyGroup group = findById(groupId);
        ensureOwner(owner, group);
        if (group.memberCount() >= group.getMaxMembers()) {
            group.setStatus(GroupStatus.FULL);
        } else {
            group.setStatus(GroupStatus.OPEN);
        }
    }

    @Transactional(readOnly = true)
    public List<GroupMembership> members(StudyGroup group) {
        return membershipRepository.findByStudyGroupOrderByJoinedAt(group);
    }

    @Transactional(readOnly = true)
    public List<GroupMembership> membershipsFor(User user) {
        return membershipRepository.findByUserOrderByJoinedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public List<JoinRequest> pendingRequests(StudyGroup group) {
        return joinRequestRepository.findByStudyGroupAndStatusOrderByCreatedAtAsc(group, RequestStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<JoinRequest> requestsFor(User user) {
        return joinRequestRepository.findByRequesterOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public boolean isMember(User user, StudyGroup group) {
        return membershipRepository.existsByStudyGroupAndUser(group, user);
    }

    @Transactional(readOnly = true)
    public boolean isOwner(User user, StudyGroup group) {
        return group.getOwner().equals(user);
    }

    private JoinRequest findRequest(Long requestId) {
        return joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Join request not found."));
    }

    private User managedUser(User user) {
        return userRepository.findById(user.getId())
                .orElseThrow(() -> new NotFoundException("User not found."));
    }

    private void ensureOwner(User user, StudyGroup group) {
        User managedUser = managedUser(user);
        if (!group.getOwner().equals(managedUser)) {
            throw new AccessDeniedException("Only the group owner can perform this action.");
        }
    }

    private void ensurePending(JoinRequest request) {
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("This request has already been reviewed.");
        }
    }
}
