package com.example.studylink.chat;

import com.example.studylink.common.CurrentUser;
import com.example.studylink.group.StudyGroupService;
import com.example.studylink.user.UserService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ChatController {

    private static final String FLASH_ERROR = "error";
    private static final String CHAT_REDIRECT_PREFIX = "redirect:/groups/";
    private static final String CHAT_REDIRECT_SUFFIX = "/chat";
    private static final String MESSAGE_VALIDATION_ERROR = "Message cannot be blank or longer than 1000 characters.";

    private final ChatService chatService;
    private final StudyGroupService studyGroupService;
    private final UserService userService;
    private final CurrentUser currentUser;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(ChatService chatService,
            StudyGroupService studyGroupService,
            UserService userService,
            CurrentUser currentUser,
            SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.studyGroupService = studyGroupService;
        this.userService = userService;
        this.currentUser = currentUser;
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping("/groups/{groupId}/chat")
    public String chat(@PathVariable Long groupId, Model model) {
        var user = currentUser.require();
        model.addAttribute("group", studyGroupService.findById(groupId));
        model.addAttribute("messages", chatService.history(user, groupId));
        model.addAttribute("chatMessageForm", new ChatMessageForm());
        return "chat/room";
    }

    @PostMapping("/groups/{groupId}/chat")
    public String send(@PathVariable Long groupId,
            @Valid @ModelAttribute ChatMessageForm chatMessageForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(FLASH_ERROR, MESSAGE_VALIDATION_ERROR);
            return redirectToChat(groupId);
        }
        chatService.sendMessage(currentUser.require(), groupId, chatMessageForm.getContent());
        return redirectToChat(groupId);
    }

    @MessageMapping("/groups/{groupId}/chat")
    public void sendOverWebSocket(@DestinationVariable Long groupId, ChatMessageForm payload, Principal principal) {
        var sender = userService.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Unknown sender."));
        ChatMessage message = chatService.sendMessage(sender, groupId, payload.getContent());
        messagingTemplate.convertAndSend("/topic/groups/" + groupId, ChatMessageView.from(message));
    }

    private static String redirectToChat(Long groupId) {
        return CHAT_REDIRECT_PREFIX + groupId + CHAT_REDIRECT_SUFFIX;
    }
}
