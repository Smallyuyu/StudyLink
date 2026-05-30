package com.example.studylink.chat;

import com.example.studylink.common.NotFoundException;
import com.example.studylink.group.StudyGroup;
import com.example.studylink.group.StudyGroupService;
import com.example.studylink.user.User;
import com.example.studylink.user.UserService;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final StudyGroupService studyGroupService;
    private final UserService userService;

    public ChatService(ChatMessageRepository chatMessageRepository,
            StudyGroupService studyGroupService,
            UserService userService) {
        this.chatMessageRepository = chatMessageRepository;
        this.studyGroupService = studyGroupService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> history(User user, Long groupId) {
        User managedUser = userService.findById(user.getId());
        StudyGroup group = studyGroupService.findById(groupId);
        ensureMember(managedUser, group);
        return chatMessageRepository.findByStudyGroupOrderBySentAtAsc(group);
    }

    @Transactional
    public ChatMessage sendMessage(User sender, Long groupId, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message cannot be blank.");
        }
        User managedSender = userService.findById(sender.getId());
        StudyGroup group = studyGroupService.findById(groupId);
        ensureMember(managedSender, group);
        return chatMessageRepository.save(new ChatMessage(group, managedSender, content.trim()));
    }

    private void ensureMember(User user, StudyGroup group) {
        if (!studyGroupService.isMember(user, group)) {
            throw new AccessDeniedException("Only group members can use this chat.");
        }
    }
}
