package com.example.studylink.common;

import com.example.studylink.user.User;
import com.example.studylink.user.UserService;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    private final UserService userService;

    public CurrentUser(UserService userService) {
        this.userService = userService;
    }

    public Optional<User> get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }
        return userService.findByEmail(authentication.getName());
    }

    public User require() {
        return get().orElseThrow(() -> new IllegalStateException("A signed-in user is required."));
    }
}
