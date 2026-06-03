package com.example.studylink.course;

import com.example.studylink.common.CurrentUser;
import com.example.studylink.group.StudyGroupService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CourseController {

    private final CourseService courseService;
    private final StudyGroupService studyGroupService;
    private final CurrentUser currentUser;

    public CourseController(CourseService courseService, StudyGroupService studyGroupService, CurrentUser currentUser) {
        this.courseService = courseService;
        this.studyGroupService = studyGroupService;
        this.currentUser = currentUser;
    }

    @GetMapping("/courses")
    public String courses(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("courses", courseService.listCourses(q));
        model.addAttribute("query", q);
        return "course/list";
    }

    @GetMapping("/courses/{courseId}")
    public String course(@PathVariable Long courseId, Model model) {
        Course course = courseService.findById(courseId);
        model.addAttribute("course", course);
        model.addAttribute("enrolled", courseService.isEnrolled(currentUser.require(), course));
        model.addAttribute("classmates", courseService.classmates(currentUser.require(), courseId));
        model.addAttribute("groups", studyGroupService.listGroupsForCourse(course));
        return "course/detail";
    }

    @PostMapping("/courses/{courseId}/enroll")
    public String enroll(@PathVariable Long courseId, RedirectAttributes redirectAttributes) {
        courseService.enroll(currentUser.require(), courseId);
        redirectAttributes.addFlashAttribute("success", "Course added to your profile.");
        return "redirect:/courses/" + courseId;
    }
}
