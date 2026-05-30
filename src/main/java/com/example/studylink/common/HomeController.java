package com.example.studylink.common;

import com.example.studylink.course.CourseService;
import com.example.studylink.group.StudyGroupService;
import java.util.Map;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final CurrentUser currentUser;
    private final CourseService courseService;
    private final StudyGroupService studyGroupService;

    public HomeController(CurrentUser currentUser, CourseService courseService, StudyGroupService studyGroupService) {
        this.currentUser = currentUser;
        this.courseService = courseService;
        this.studyGroupService = studyGroupService;
    }

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        var user = currentUser.require();
        model.addAttribute("user", user);
        model.addAttribute("courses", user.getEnrolledCourses());
        model.addAttribute("memberships", studyGroupService.membershipsFor(user));
        model.addAttribute("requests", studyGroupService.requestsFor(user));
        model.addAttribute("recommendedGroups", studyGroupService.openGroups().stream().limit(3).toList());
        return "dashboard";
    }

    @GetMapping("/health")
    @ResponseBody
    public Map<String, String> health() {
        return Map.of("status", "UP", "application", "StudyLink");
    }
}
