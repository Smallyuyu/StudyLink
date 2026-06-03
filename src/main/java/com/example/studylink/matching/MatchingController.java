package com.example.studylink.matching;

import com.example.studylink.common.CurrentUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MatchingController {

    private final MatchingService matchingService;
    private final CurrentUser currentUser;

    public MatchingController(MatchingService matchingService, CurrentUser currentUser) {
        this.matchingService = matchingService;
        this.currentUser = currentUser;
    }

    @GetMapping("/matches")
    public String matches(Model model) {
        model.addAttribute("matches", matchingService.recommendGroups(currentUser.require()));
        return "matching/list";
    }
}
