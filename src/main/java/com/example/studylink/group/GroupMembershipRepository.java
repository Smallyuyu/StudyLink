package com.example.studylink.group;

import com.example.studylink.user.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMembershipRepository extends JpaRepository<GroupMembership, Long> {

    boolean existsByStudyGroupAndUser(StudyGroup studyGroup, User user);

    Optional<GroupMembership> findByStudyGroupAndUser(StudyGroup studyGroup, User user);

    List<GroupMembership> findByStudyGroupOrderByJoinedAt(StudyGroup studyGroup);

    List<GroupMembership> findByUserOrderByJoinedAtDesc(User user);
}
