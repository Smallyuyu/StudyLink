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

    private static final int SAME_COURSE_POINTS = 50;
    private static final int SKILL_OVERLAP_POINTS = 10;
    private static final int TIME_OVERLAP_POINTS = 15;
    private static final int GOAL_OVERLAP_POINTS = 8;
    private static final int OPEN_SLOT_POINTS = 20;

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
            score += SAME_COURSE_POINTS;
            addReason(reasons, SAME_COURSE_POINTS, "same course: " + group.getCourse().getCode());
        }

        Set<String> groupGoalTokens = tokens(safeText(group.getTargetGoals())
                + " " + safeText(group.getDescription()));
        Set<String> skillOverlap = overlap(tokens(user.getSkillTags()), groupGoalTokens);
        score += addOverlapReason(reasons, "overlapping skills", skillOverlap, SKILL_OVERLAP_POINTS);

        Set<String> timeOverlap = overlap(tokens(user.getAvailableTimes()), tokens(group.getPreferredTimes()));
        score += addOverlapReason(reasons, "matching times", timeOverlap, TIME_OVERLAP_POINTS);

        Set<String> goalOverlap = overlap(tokens(user.getLearningGoals()), groupGoalTokens);
        score += addOverlapReason(reasons, "learning goal overlap", goalOverlap, GOAL_OVERLAP_POINTS);

        if (group.hasOpenSlot()) {
            score += OPEN_SLOT_POINTS;
            addReason(reasons, OPEN_SLOT_POINTS, "open slot available");
        }

        return new MatchResult(group, score, List.copyOf(reasons));
    }

    private static int addOverlapReason(List<String> reasons, String label, Set<String> matchingTokens,
            int pointsPerToken) {
        if (matchingTokens.isEmpty()) {
            return 0;
        }

        int points = matchingTokens.size() * pointsPerToken;
        addReason(reasons, points, label + ": " + String.join(", ", matchingTokens));
        return points;
    }

    private static void addReason(List<String> reasons, int points, String reason) {
        reasons.add("+" + points + " " + reason);
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

    private static String safeText(String text) {
        return text == null ? "" : text;
    }

    private static Set<String> overlap(Set<String> left, Set<String> right) {
        Set<String> values = new LinkedHashSet<>(left);
        values.retainAll(right);
        return values;
    }
}
