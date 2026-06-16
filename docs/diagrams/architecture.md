# Architecture

This diagram set describes the `master` branch implementation: a Spring Boot
MVC application with Thymeleaf pages, Spring Security authentication, JPA/H2
persistence, rule-based recommendations, and WebSocket/STOMP group chat.

## System Context

```mermaid
flowchart TD
    Student["Student browser"] --> Pages["Thymeleaf pages"]
    Pages --> SpringMvc["Spring MVC controllers"]
    SpringMvc --> Security["Spring Security"]
    SpringMvc --> Services["Application services"]
    Services --> Repositories["Spring Data JPA repositories"]
    Repositories --> H2["H2 database"]
    Pages --> SockJs["SockJS/STOMP client"]
    SockJs --> WebSocket["/ws endpoint"]
    WebSocket --> Broker["Simple broker /topic"]
    WebSocket --> ChatController["ChatController @MessageMapping"]
    ChatController --> ChatService["ChatService"]
    ChatService --> Repositories
    Broker --> SockJs
```

## Package Flow

```mermaid
flowchart LR
    subgraph UI["Templates and static assets"]
        Layout["layout.html"]
        Views["home/dashboard/user/course/group/matching/chat views"]
        Css["app.css"]
    end

    subgraph Web["Web layer"]
        HomeController
        UserController
        CourseController
        StudyGroupController
        MatchingController
        ChatController
        GlobalExceptionHandler
    end

    subgraph App["Service layer"]
        UserService
        CourseService
        StudyGroupService
        MatchingService
        ChatService
        CurrentUser
    end

    subgraph Data["Data layer"]
        UserRepository
        CourseRepository
        StudyGroupRepository
        GroupMembershipRepository
        JoinRequestRepository
        ChatMessageRepository
    end

    UI --> Web
    Web --> App
    App --> Data
    Data --> H2["H2"]
    SecurityConfig["SecurityConfig"] --> Web
    WebSocketConfig["WebSocketConfig"] --> ChatController
    DemoDataInitializer["DemoDataInitializer"] --> Data
```

## HTTP Request Flow

```mermaid
flowchart TD
    Request["HTTP request"] --> Filter["Spring Security filter chain"]
    Filter --> Public{"Public URL?"}
    Public -- yes --> PublicControllers["Home/User registration/login"]
    Public -- no --> Authenticated{"Authenticated?"}
    Authenticated -- no --> Login["/login"]
    Authenticated -- yes --> CurrentUser["CurrentUser resolves User"]
    CurrentUser --> Controller["Feature controller"]
    PublicControllers --> Controller
    Controller --> FormValidation["Form validation and model binding"]
    FormValidation --> Service["Service transaction boundary"]
    Service --> Repository["Repository query/save"]
    Repository --> Database["H2 database"]
    Service --> ViewModel["Model attributes / redirect flash"]
    ViewModel --> Template["Thymeleaf template"]
```

## Domain Model

```mermaid
classDiagram
    class User {
        Long id
        String email
        String passwordHash
        String displayName
        String department
        String skillTags
        String availableTimes
        String learningGoals
        Set~Course~ enrolledCourses
        updateProfile(ProfileForm)
        enroll(Course)
    }

    class Course {
        Long id
        String code
        String name
        String instructor
        String semester
        Set~User~ students
        Set~StudyGroup~ studyGroups
    }

    class StudyGroup {
        Long id
        String name
        String description
        Integer maxMembers
        String targetGoals
        String preferredTimes
        GroupStatus status
        memberCount()
        hasOpenSlot()
        markFullIfNeeded()
        markOpenIfSeatAvailable()
    }

    class GroupMembership {
        Long id
        GroupRole role
        LocalDateTime joinedAt
    }

    class JoinRequest {
        Long id
        RequestStatus status
        String message
        LocalDateTime createdAt
        LocalDateTime reviewedAt
        approve()
        decline()
    }

    class ChatMessage {
        Long id
        String content
        LocalDateTime sentAt
    }

    class GroupStatus {
        <<enumeration>>
        OPEN
        FULL
        CLOSED
        DELETED
    }

    class GroupRole {
        <<enumeration>>
        OWNER
        MEMBER
    }

    class RequestStatus {
        <<enumeration>>
        PENDING
        APPROVED
        DECLINED
    }

    User "many" -- "many" Course : enrolls
    Course "1" -- "0..*" StudyGroup : has
    User "1" -- "0..*" StudyGroup : owns
    StudyGroup "1" -- "1..*" GroupMembership : memberships
    User "1" -- "0..*" GroupMembership : joins
    StudyGroup "1" -- "0..*" JoinRequest : receives
    User "1" -- "0..*" JoinRequest : requests
    StudyGroup "1" -- "0..*" ChatMessage : contains
    User "1" -- "0..*" ChatMessage : sends
    StudyGroup --> GroupStatus
    GroupMembership --> GroupRole
    JoinRequest --> RequestStatus
```

## Study Group Lifecycle

```mermaid
stateDiagram-v2
    [*] --> OPEN: createGroup()
    OPEN --> FULL: markFullIfNeeded()
    FULL --> OPEN: markOpenIfSeatAvailable()
    OPEN --> CLOSED: closeGroup()
    FULL --> CLOSED: closeGroup()
    CLOSED --> OPEN: reopenGroup() with seats
    CLOSED --> FULL: reopenGroup() without seats
    OPEN --> DELETED: deleteGroup()
    FULL --> DELETED: deleteGroup()
    CLOSED --> DELETED: deleteGroup()
    DELETED --> [*]
```

## Join Request Flow

```mermaid
sequenceDiagram
    actor Requester
    participant GroupPage as StudyGroupController
    participant GroupService as StudyGroupService
    participant RequestRepo as JoinRequestRepository
    participant Owner

    Requester->>GroupPage: POST /groups/{groupId}/join
    GroupPage->>GroupService: requestToJoin(user, groupId, form)
    GroupService->>GroupService: ensure open slot, non-member, no pending request
    GroupService->>RequestRepo: save(PENDING request)
    RequestRepo-->>GroupService: JoinRequest
    GroupService-->>GroupPage: request saved
    Owner->>GroupPage: POST /groups/requests/{requestId}/approve
    GroupPage->>GroupService: approveRequest(owner, requestId)
    GroupService->>GroupService: ensure owner and pending status
    GroupService->>GroupService: add MEMBER membership
    GroupService->>GroupService: mark FULL if needed
    GroupService-->>GroupPage: APPROVED request
```

## Matching Score

```mermaid
flowchart TD
    UserProfile["Managed User profile"] --> Criteria["MatchCriteria"]
    Criteria --> Tokens["Tokenize skills, times, goals"]
    OpenGroups["StudyGroupService.openGroups()"] --> ExcludeMembers["Exclude groups where user is already a member"]
    ExcludeMembers --> Score["MatchingService.score()"]
    Tokens --> Score
    Score --> SameCourse["+50 same course"]
    Score --> SkillOverlap["+10 each overlapping skill token"]
    Score --> TimeOverlap["+15 each matching time token"]
    Score --> GoalOverlap["+8 each goal token"]
    Score --> OpenSlot["+20 open slot"]
    SameCourse --> Result["MatchResult"]
    SkillOverlap --> Result
    TimeOverlap --> Result
    GoalOverlap --> Result
    OpenSlot --> Result
    Result --> Ranked["Sort by score descending"]
    Ranked --> Reasons["Display score and human-readable reasons"]
```

## Chat Flow

```mermaid
sequenceDiagram
    actor Member
    participant Page as chat/room.html
    participant SockJs as SockJS/STOMP
    participant ChatController
    participant ChatService
    participant GroupService as StudyGroupService
    participant MessageRepo as ChatMessageRepository
    participant Topic as /topic/groups/{groupId}

    Member->>Page: open /groups/{groupId}/chat
    Page->>ChatController: GET chat room
    ChatController->>ChatService: history(user, groupId)
    ChatService->>GroupService: requireActiveMembership(user, groupId)
    ChatService->>MessageRepo: findByStudyGroupOrderBySentAtAsc(group)
    Page->>SockJs: connect /ws
    Member->>SockJs: SEND /app/groups/{groupId}/chat
    SockJs->>ChatController: @MessageMapping
    ChatController->>ChatService: sendMessage(user, groupId, content)
    ChatService->>GroupService: requireActiveMembership(user, groupId)
    ChatService->>MessageRepo: save(ChatMessage)
    ChatController->>Topic: publish ChatMessageView
    Topic-->>Page: live message update
```
