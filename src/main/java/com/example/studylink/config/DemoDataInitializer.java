package com.example.studylink.config;

import com.example.studylink.chat.ChatMessage;
import com.example.studylink.chat.ChatMessageRepository;
import com.example.studylink.course.Course;
import com.example.studylink.course.CourseRepository;
import com.example.studylink.group.GroupMembership;
import com.example.studylink.group.GroupMembershipRepository;
import com.example.studylink.group.GroupRole;
import com.example.studylink.group.JoinRequest;
import com.example.studylink.group.JoinRequestRepository;
import com.example.studylink.group.StudyGroup;
import com.example.studylink.group.StudyGroupRepository;
import com.example.studylink.user.ProfileForm;
import com.example.studylink.user.User;
import com.example.studylink.user.UserRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("!test")
public class DemoDataInitializer {

    @Bean
    CommandLineRunner seedDemoData(UserRepository userRepository,
            CourseRepository courseRepository,
            StudyGroupRepository groupRepository,
            GroupMembershipRepository membershipRepository,
            JoinRequestRepository joinRequestRepository,
            ChatMessageRepository chatMessageRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() > 0) {
                return;
            }

            Course dataScience = courseRepository.save(new Course("CS501", "Data Science Studio", "Dr. Lin", "Spring 2026"));
            Course algorithms = courseRepository.save(new Course("CS210", "Algorithms", "Prof. Chen", "Spring 2026"));
            Course design = courseRepository.save(new Course("UX330", "Human-Centered Design", "Dr. Wu", "Spring 2026"));
            Course backend = courseRepository.save(new Course("CS410", "Cloud Backend Systems", "Prof. Huang", "Spring 2026"));

            User alice = demoUser(userRepository, passwordEncoder, "alice@studylink.test", "Alice Lin",
                    "Computer Science", "python, data, sql, visualization", "monday, wednesday, night",
                    "machine learning, portfolio, data storytelling");
            User ben = demoUser(userRepository, passwordEncoder, "ben@studylink.test", "Ben Wu",
                    "Information Management", "java, spring, sql, backend", "tuesday, thursday, night",
                    "backend systems, clean architecture, interview practice");
            User clara = demoUser(userRepository, passwordEncoder, "clara@studylink.test", "Clara Huang",
                    "Design", "figma, research, frontend, accessibility", "monday, friday, afternoon",
                    "user research, design systems, prototype critique");
            User devin = demoUser(userRepository, passwordEncoder, "devin@studylink.test", "Devin Kao",
                    "Computer Science", "algorithms, python, math, proofs", "wednesday, saturday, morning",
                    "algorithm drills, contest practice, proofs");
            User erin = demoUser(userRepository, passwordEncoder, "erin@studylink.test", "Erin Chen",
                    "Business", "product, analytics, sql, presentation", "thursday, sunday, night",
                    "product analytics, data storytelling, case practice");

            enroll(alice, dataScience, algorithms);
            enroll(ben, backend, dataScience);
            enroll(clara, design, dataScience);
            enroll(devin, algorithms, dataScience);
            enroll(erin, dataScience, design);
            userRepository.saveAll(List.of(alice, ben, clara, devin, erin));

            StudyGroup mlGroup = groupRepository.save(new StudyGroup(
                    "ML Portfolio Sprint",
                    "Weekly sessions for notebooks, model review, and visual explanations.",
                    5,
                    "machine learning, python, data, portfolio, storytelling",
                    "monday, wednesday, night",
                    dataScience,
                    alice));
            StudyGroup backendGroup = groupRepository.save(new StudyGroup(
                    "Spring Backend Lab",
                    "Build small services and review clean architecture patterns together.",
                    4,
                    "java, spring, backend, sql, architecture",
                    "tuesday, thursday, night",
                    backend,
                    ben));
            StudyGroup algorithmsGroup = groupRepository.save(new StudyGroup(
                    "Algorithm Drill Circle",
                    "Timed problem sets, proof discussion, and contest review.",
                    4,
                    "algorithms, math, proofs, contest",
                    "wednesday, saturday, morning",
                    algorithms,
                    devin));
            StudyGroup uxGroup = groupRepository.save(new StudyGroup(
                    "Research Critique Studio",
                    "Trade interview scripts, prototype critique, and accessibility notes.",
                    3,
                    "research, prototype, accessibility, design systems",
                    "monday, friday, afternoon",
                    design,
                    clara));

            membershipRepository.save(new GroupMembership(mlGroup, alice, GroupRole.OWNER));
            membershipRepository.save(new GroupMembership(mlGroup, erin, GroupRole.MEMBER));
            membershipRepository.save(new GroupMembership(backendGroup, ben, GroupRole.OWNER));
            membershipRepository.save(new GroupMembership(algorithmsGroup, devin, GroupRole.OWNER));
            membershipRepository.save(new GroupMembership(uxGroup, clara, GroupRole.OWNER));

            joinRequestRepository.save(new JoinRequest(mlGroup, ben, "I can help with backend deployment and SQL cleanup."));
            joinRequestRepository.save(new JoinRequest(uxGroup, erin, "I want more practice turning research into product decisions."));

            chatMessageRepository.save(new ChatMessage(mlGroup, alice, "Welcome. Bring one dataset idea and one question this week."));
            chatMessageRepository.save(new ChatMessage(mlGroup, erin, "I can prepare a short product analytics example."));
        };
    }

    private static User demoUser(UserRepository repository,
            PasswordEncoder passwordEncoder,
            String email,
            String displayName,
            String department,
            String skillTags,
            String availableTimes,
            String learningGoals) {
        User user = new User(email, passwordEncoder.encode("password"), displayName);
        ProfileForm form = new ProfileForm();
        form.setDisplayName(displayName);
        form.setDepartment(department);
        form.setSkillTags(skillTags);
        form.setAvailableTimes(availableTimes);
        form.setLearningGoals(learningGoals);
        user.updateProfile(form);
        return repository.save(user);
    }

    private static void enroll(User user, Course... courses) {
        for (Course course : courses) {
            user.enroll(course);
        }
    }
}
