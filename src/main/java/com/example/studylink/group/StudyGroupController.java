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

    private static final String FLASH_ERROR = "error";
    private static final String FLASH_SUCCESS = "success";
    private static final String GROUP_LIST_REDIRECT = "redirect:/groups";
    private static final String GROUP_DETAIL_REDIRECT_PREFIX = "redirect:/groups/";

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
        model.addAttribute("currentUser", currentUser.require());
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
        success(redirectAttributes, "Study group created.");
        return redirectToGroup(group.getId());
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
            error(redirectAttributes, "Join request message is too long.");
            return redirectToGroup(groupId);
        }
        studyGroupService.requestToJoin(currentUser.require(), groupId, joinRequestForm);
        success(redirectAttributes, "Join request sent.");
        return redirectToGroup(groupId);
    }

    @PostMapping("/groups/requests/{requestId}/approve")
    public String approveRequest(@PathVariable Long requestId, RedirectAttributes redirectAttributes) {
        JoinRequest request = studyGroupService.approveRequest(currentUser.require(), requestId);
        success(redirectAttributes, "Request approved.");
        return redirectToGroup(request.getStudyGroup().getId());
    }

    @PostMapping("/groups/requests/{requestId}/decline")
    public String declineRequest(@PathVariable Long requestId, RedirectAttributes redirectAttributes) {
        JoinRequest request = studyGroupService.declineRequest(currentUser.require(), requestId);
        success(redirectAttributes, "Request declined.");
        return redirectToGroup(request.getStudyGroup().getId());
    }

    @PostMapping("/groups/{groupId}/close")
    public String closeGroup(@PathVariable Long groupId, RedirectAttributes redirectAttributes) {
        studyGroupService.closeGroup(currentUser.require(), groupId);
        success(redirectAttributes, "Group closed.");
        return redirectToGroup(groupId);
    }

    @PostMapping("/groups/{groupId}/reopen")
    public String reopenGroup(@PathVariable Long groupId, RedirectAttributes redirectAttributes) {
        studyGroupService.reopenGroup(currentUser.require(), groupId);
        success(redirectAttributes, "Group reopened.");
        return redirectToGroup(groupId);
    }

    @PostMapping("/groups/{groupId}/delete")
    public String deleteGroup(@PathVariable Long groupId, RedirectAttributes redirectAttributes) {
        studyGroupService.deleteGroup(currentUser.require(), groupId);
        success(redirectAttributes, "Group deleted.");
        return GROUP_LIST_REDIRECT;
    }

    @PostMapping("/groups/{groupId}/leave")
    public String leaveGroup(@PathVariable Long groupId, RedirectAttributes redirectAttributes) {
        studyGroupService.leaveGroup(currentUser.require(), groupId);
        success(redirectAttributes, "You left the group.");
        return GROUP_LIST_REDIRECT;
    }

    @PostMapping("/groups/{groupId}/members/{membershipId}/remove")
    public String removeMember(@PathVariable Long groupId,
            @PathVariable Long membershipId,
            RedirectAttributes redirectAttributes) {
        studyGroupService.removeMember(currentUser.require(), groupId, membershipId);
        success(redirectAttributes, "Member removed.");
        return redirectToGroup(groupId);
    }

    @PostMapping("/groups/{groupId}/members/{membershipId}/transfer-owner")
    public String transferOwner(@PathVariable Long groupId,
            @PathVariable Long membershipId,
            RedirectAttributes redirectAttributes) {
        studyGroupService.transferOwnership(currentUser.require(), groupId, membershipId);
        success(redirectAttributes, "Group owner transferred.");
        return redirectToGroup(groupId);
    }

    private static void success(RedirectAttributes redirectAttributes, String message) {
        redirectAttributes.addFlashAttribute(FLASH_SUCCESS, message);
    }

    private static void error(RedirectAttributes redirectAttributes, String message) {
        redirectAttributes.addFlashAttribute(FLASH_ERROR, message);
    }

    private static String redirectToGroup(Long groupId) {
        return GROUP_DETAIL_REDIRECT_PREFIX + groupId;
    }
}
