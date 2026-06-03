package com.example.studylink.group;

import com.example.studylink.chat.ChatMessage;
import com.example.studylink.course.Course;
import com.example.studylink.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "study_groups")
public class StudyGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1200)
    private String description;

    @Column(nullable = false)
    private Integer maxMembers;

    @Column(length = 1000)
    private String targetGoals;

    @Column(length = 1000)
    private String preferredTimes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupStatus status = GroupStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User owner;

    @OneToMany(mappedBy = "studyGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<GroupMembership> memberships = new LinkedHashSet<>();

    @OneToMany(mappedBy = "studyGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<JoinRequest> joinRequests = new LinkedHashSet<>();

    @OneToMany(mappedBy = "studyGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ChatMessage> messages = new LinkedHashSet<>();

    protected StudyGroup() {
    }

    public StudyGroup(String name, String description, Integer maxMembers, String targetGoals,
            String preferredTimes, Course course, User owner) {
        this.name = name;
        this.description = description;
        this.maxMembers = maxMembers;
        this.targetGoals = targetGoals;
        this.preferredTimes = preferredTimes;
        this.course = course;
        this.owner = owner;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Integer getMaxMembers() {
        return maxMembers;
    }

    public String getTargetGoals() {
        return targetGoals;
    }

    public String getPreferredTimes() {
        return preferredTimes;
    }

    public GroupStatus getStatus() {
        return status;
    }

    public Course getCourse() {
        return course;
    }

    public User getOwner() {
        return owner;
    }

    public Set<GroupMembership> getMemberships() {
        return memberships;
    }

    public Set<JoinRequest> getJoinRequests() {
        return joinRequests;
    }

    public Set<ChatMessage> getMessages() {
        return messages;
    }

    public int memberCount() {
        return memberships.size();
    }

    public boolean hasOpenSlot() {
        return status == GroupStatus.OPEN && memberCount() < maxMembers;
    }

    public boolean isDeleted() {
        return status == GroupStatus.DELETED;
    }

    public void markFullIfNeeded() {
        if (memberCount() >= maxMembers) {
            status = GroupStatus.FULL;
        }
    }

    public void markOpenIfSeatAvailable() {
        if (status == GroupStatus.FULL && memberCount() < maxMembers) {
            status = GroupStatus.OPEN;
        }
    }

    public void transferOwnership(User owner) {
        this.owner = owner;
    }

    public void setStatus(GroupStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StudyGroup that)) {
            return false;
        }
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
