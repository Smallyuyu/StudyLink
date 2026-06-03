package com.example.studylink.chat;

import com.example.studylink.group.GroupMembership;
import com.example.studylink.group.StudyGroup;
import com.example.studylink.group.StudyGroupService;
import com.example.studylink.user.User;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final StudyGroupService studyGroupService;

    public ChatService(ChatMessageRepository chatMessageRepository,
            StudyGroupService studyGroupService) {
        this.chatMessageRepository = chatMessageRepository;
        this.studyGroupService = studyGroupService;
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> history(User user, Long groupId) {
        StudyGroup group = studyGroupService.requireActiveMembership(user, groupId).getStudyGroup();
        return chatMessageRepository.findByStudyGroupOrderBySentAtAsc(group);
    }

    @Transactional
    public ChatMessage sendMessage(User sender, Long groupId, String content) {
        String normalizedContent = normalizedContent(content);
        GroupMembership membership = studyGroupService.requireActiveMembership(sender, groupId);
        return chatMessageRepository.save(new ChatMessage(
                membership.getStudyGroup(),
                membership.getUser(),
                normalizedContent));
    }

    private String normalizedContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message cannot be blank.");
        }

        String normalizedContent = content.trim();
        if (normalizedContent.length() > ChatMessageForm.MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("Message cannot be longer than 1000 characters.");
        }
        return normalizedContent;
    }
}
