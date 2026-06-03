package com.example.studylink.chat;

import com.example.studylink.group.StudyGroup;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByStudyGroupOrderBySentAtAsc(StudyGroup studyGroup);
}
