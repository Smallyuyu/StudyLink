package com.example.studylink.chat;

import java.time.LocalDateTime;

public record ChatMessageView(Long id, String senderName, String content, LocalDateTime sentAt) {

    public static ChatMessageView from(ChatMessage message) {
        return new ChatMessageView(
                message.getId(),
                message.getSender().getDisplayName(),
                message.getContent(),
                message.getSentAt());
    }
}
