package com.example.studylink.group;

import jakarta.validation.constraints.Size;

public class JoinRequestForm {

    public static final int MAX_MESSAGE_LENGTH = 600;

    @Size(max = MAX_MESSAGE_LENGTH)
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
