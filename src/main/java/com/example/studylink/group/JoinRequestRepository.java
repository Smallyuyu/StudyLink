package com.example.studylink.group;

import com.example.studylink.user.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JoinRequestRepository extends JpaRepository<JoinRequest, Long> {

    boolean existsByStudyGroupAndRequesterAndStatus(StudyGroup studyGroup, User requester, RequestStatus status);

    List<JoinRequest> findByStudyGroupAndStatusOrderByCreatedAtAsc(StudyGroup studyGroup, RequestStatus status);

    List<JoinRequest> findByRequesterOrderByCreatedAtDesc(User requester);
}
