package com.example.studylink.group;

import com.example.studylink.common.CurrentUser;
import com.example.studylink.course.CourseService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class StudyGroupController {

    private final StudyGroupService studyGroupService;
    private final CourseService courseService;
    private final CurrentUser currentUser;

    public StudyGroupController(StudyGroupService studyGroupService,
            CourseService courseService,
            CurrentUser currentUser) {
        this.studyGroupService = studyGroupService;
        this.courseService = courseService;
        this.currentUser = currentUser;
    }

    @GetMapping("/groups")
    public String groups(Model model) {
        model.addAttribute("groups", studyGroupService.listGroups());
        return "group/list";
    }

    @GetMapping("/groups/new")
    public String newGroup(@RequestParam(required = false) Long courseId, Model model) {
        StudyGroupForm form = new StudyGroupForm();
        form.setCourseId(courseId);
        model.addAttribute("studyGroupForm", form);
        model.addAttribute("courses", courseService.listCourses(null));
        return "group/new";
    }

    @PostMapping("/groups")
    public String createGroup(@Valid @ModelAttribute StudyGroupForm studyGroupForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("courses", courseService.listCourses(null));
            return "group/new";
        }

        StudyGroup group = studyGroupService.createGroup(currentUser.require(), studyGroupForm);
        redirectAttributes.addFlashAttribute("success", "Study group created.");
        return "redirect:/groups/" + group.getId();
    }

    @GetMapping("/groups/{groupId}")
    public String group(@PathVariable Long groupId, Model model) {
        StudyGroup group = studyGroupService.findById(groupId);
        var user = currentUser.require();
        model.addAttribute("group", group);
        model.addAttribute("members", studyGroupService.members(group));
        model.addAttribute("joinRequestForm", new JoinRequestForm());
        model.addAttribute("isMember", studyGroupService.isMember(user, group));
        model.addAttribute("isOwner", studyGroupService.isOwner(user, group));
        model.addAttribute("pendingRequests", studyGroupService.pendingRequests(group));
        return "group/detail";
    }

    @PostMapping("/groups/{groupId}/join")
    public String requestJoin(@PathVariable Long groupId,
            @Valid @ModelAttribute JoinRequestForm joinRequestForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Join request message is too long.");
            return "redirect:/groups/" + groupId;
        }
        studyGroupService.requestToJoin(currentUser.require(), groupId, joinRequestForm);
        redirectAttributes.addFlashAttribute("success", "Join request sent.");
        return "redirect:/groups/" + groupId;
    }

    @PostMapping("/groups/requests/{requestId}/approve")
    public String approveRequest(@PathVariable Long requestId, RedirectAttributes redirectAttributes) {
        JoinRequest request = studyGroupService.approveRequest(currentUser.require(), requestId);
        redirectAttributes.addFlashAttribute("success", "Request approved.");
        return "redirect:/groups/" + request.getStudyGroup().getId();
    }

    @PostMapping("/groups/requests/{requestId}/decline")
    public String declineRequest(@PathVariable Long requestId, RedirectAttributes redirectAttributes) {
        JoinRequest request = studyGroupService.declineRequest(currentUser.require(), requestId);
        redirectAttributes.addFlashAttribute("success", "Request declined.");
        return "redirect:/groups/" + request.getStudyGroup().getId();
    }

    @PostMapping("/groups/{groupId}/close")
    public String closeGroup(@PathVariable Long groupId, RedirectAttributes redirectAttributes) {
        studyGroupService.closeGroup(currentUser.require(), groupId);
        redirectAttributes.addFlashAttribute("success", "Group closed.");
        return "redirect:/groups/" + groupId;
    }

    @PostMapping("/groups/{groupId}/reopen")
    public String reopenGroup(@PathVariable Long groupId, RedirectAttributes redirectAttributes) {
        studyGroupService.reopenGroup(currentUser.require(), groupId);
        redirectAttributes.addFlashAttribute("success", "Group reopened.");
        return "redirect:/groups/" + groupId;
    }
}
