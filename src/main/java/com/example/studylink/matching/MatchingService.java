package com.example.studylink.matching;

import com.example.studylink.group.StudyGroup;
import com.example.studylink.group.StudyGroupService;
import com.example.studylink.user.User;
import com.example.studylink.user.UserService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchingService {

    private final StudyGroupService studyGroupService;
    private final UserService userService;

    public MatchingService(StudyGroupService studyGroupService, UserService userService) {
        this.studyGroupService = studyGroupService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<MatchResult> recommendGroups(User user) {
        User managedUser = userService.findById(user.getId());
        MatchCriteria criteria = new MatchCriteria(managedUser);
        return studyGroupService.openGroups().stream()
                .filter(group -> !studyGroupService.isMember(managedUser, group))
                .map(group -> score(criteria, group))
                .filter(result -> result.score() > 0)
                .sorted((left, right) -> Integer.compare(right.score(), left.score()))
                .toList();
    }

    MatchResult score(MatchCriteria criteria, StudyGroup group) {
        User user = criteria.user();
        int score = 0;
        List<String> reasons = new ArrayList<>();

        if (user.getEnrolledCourses().contains(group.getCourse())) {
            score += 50;
            reasons.add("+50 same course: " + group.getCourse().getCode());
        }

        Set<String> groupGoalTokens = tokens(group.getTargetGoals() + " " + group.getDescription());
        Set<String> skillOverlap = overlap(tokens(user.getSkillTags()), groupGoalTokens);
        if (!skillOverlap.isEmpty()) {
            int points = skillOverlap.size() * 10;
            score += points;
            reasons.add("+" + points + " overlapping skills: " + String.join(", ", skillOverlap));
        }

        Set<String> timeOverlap = overlap(tokens(user.getAvailableTimes()), tokens(group.getPreferredTimes()));
        if (!timeOverlap.isEmpty()) {
            int points = timeOverlap.size() * 15;
            score += points;
            reasons.add("+" + points + " matching times: " + String.join(", ", timeOverlap));
        }

        Set<String> goalOverlap = overlap(tokens(user.getLearningGoals()), groupGoalTokens);
        if (!goalOverlap.isEmpty()) {
            int points = goalOverlap.size() * 8;
            score += points;
            reasons.add("+" + points + " learning goal overlap: " + String.join(", ", goalOverlap));
        }

        if (group.hasOpenSlot()) {
            score += 20;
            reasons.add("+20 open slot available");
        }

        return new MatchResult(group, score, List.copyOf(reasons));
    }

    private static Set<String> tokens(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        Set<String> values = new LinkedHashSet<>();
        Arrays.stream(text.toLowerCase(Locale.ROOT).split("[,;\\s/|]+"))
                .map(String::trim)
                .filter(value -> value.length() >= 2)
                .forEach(values::add);
        return values;
    }

    private static Set<String> overlap(Set<String> left, Set<String> right) {
        Set<String> values = new LinkedHashSet<>(left);
        values.retainAll(right);
        return values;
    }
}
