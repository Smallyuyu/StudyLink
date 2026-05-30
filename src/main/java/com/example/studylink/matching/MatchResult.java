package com.example.studylink.matching;

import com.example.studylink.group.StudyGroup;
import java.util.List;

public record MatchResult(StudyGroup group, int score, List<String> reasons) {
}
