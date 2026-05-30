package com.example.studylink.group;

import jakarta.validation.constraints.Size;

public class JoinRequestForm {

    @Size(max = 600)
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
