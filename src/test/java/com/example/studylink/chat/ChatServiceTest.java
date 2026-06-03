package com.example.studylink.chat;

import static com.example.studylink.TestFixtures.course;
import static com.example.studylink.TestFixtures.groupForm;
import static com.example.studylink.TestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.studylink.course.Course;
import com.example.studylink.course.CourseRepository;
import com.example.studylink.group.StudyGroup;
import com.example.studylink.group.StudyGroupService;
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
class ChatServiceTest {

    @Autowired
    private ChatService chatService;

    @Autowired
    private StudyGroupService studyGroupService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void sendMessage_nonMember_throwsAccessDeniedException() {
        Course course = courseRepository.save(course("CS400"));
        User owner = userRepository.save(user("chat-owner@example.com", "Owner", passwordEncoder));
        User stranger = userRepository.save(user("chat-stranger@example.com", "Stranger", passwordEncoder));
        StudyGroup group = studyGroupService.createGroup(owner, groupForm(course, "Chat Group"));

        assertThatThrownBy(() -> chatService.sendMessage(stranger, group.getId(), "Hello"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void sendMessage_tooLongMessage_throwsException() {
        Course course = courseRepository.save(course("CS402"));
        User owner = userRepository.save(user("chat-owner3@example.com", "Owner", passwordEncoder));
        StudyGroup group = studyGroupService.createGroup(owner, groupForm(course, "Chat Limit Group"));

        assertThatThrownBy(() -> chatService.sendMessage(owner, group.getId(),
                "x".repeat(ChatMessageForm.MAX_CONTENT_LENGTH + 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("longer than 1000");
    }

    @Test
    void sendMessage_member_persistsMessage() {
        Course course = courseRepository.save(course("CS401"));
        User owner = userRepository.save(user("chat-owner2@example.com", "Owner", passwordEncoder));
        StudyGroup group = studyGroupService.createGroup(owner, groupForm(course, "Chat Group 2"));

        ChatMessage message = chatService.sendMessage(owner, group.getId(), "First message");

        assertThat(message.getId()).isNotNull();
        assertThat(chatService.history(owner, group.getId())).extracting(ChatMessage::getContent)
                .containsExactly("First message");
    }

    @Test
    void sendMessage_deletedGroup_throwsException() {
        Course course = courseRepository.save(course("CS403"));
        User owner = userRepository.save(user("chat-owner4@example.com", "Owner", passwordEncoder));
        StudyGroup group = studyGroupService.createGroup(owner, groupForm(course, "Deleted Chat Group"));

        studyGroupService.deleteGroup(owner, group.getId());

        assertThatThrownBy(() -> chatService.sendMessage(owner, group.getId(), "Anyone here?"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deleted");
    }
}
