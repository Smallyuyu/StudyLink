package com.example.studylink.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChatMessageForm {

    public static final int MAX_CONTENT_LENGTH = 1000;

    @NotBlank
    @Size(max = MAX_CONTENT_LENGTH)
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
