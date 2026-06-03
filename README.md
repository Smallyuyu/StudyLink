# StudyLink

StudyLink is a Spring Boot MVP for students who want to find classmates, create study groups, request to join groups, get simple matching recommendations, and chat inside accepted groups.

## Features

- Account registration, login, logout, and editable learner profiles.
- Course catalog with enrollment, classmates, and course-linked study groups.
- Study group creation, owner approval for join requests, membership roles, and group status.
- Rule-based matching recommendations using course overlap, skill tags, available time, goals, and open seats.
- Group chat with persisted messages plus a WebSocket/STOMP endpoint for live updates.
- H2 database, seed data, Thymeleaf views, Bootstrap 5 styling, and focused service/controller tests.

## Tech Stack

- Java 17+
- Spring Boot 3
- Spring MVC, Thymeleaf, Bootstrap 5
- Spring Data JPA, H2
- Spring Security, BCrypt
- Spring WebSocket + STOMP
- Maven, JUnit 5, AssertJ, Mockito, Spring Boot Test

## Tree
```powershell
├─src
│  ├─main
│  │  ├─java
│  │  │  └─com
│  │  │      └─example
│  │  │          └─studylink
│  │  │              ├─chat
│  │  │              ├─common
│  │  │              ├─config
│  │  │              ├─course
│  │  │              ├─group
│  │  │              ├─matching
│  │  │              └─user
│  │  └─resources
│  │      ├─static
│  │      │  └─css
│  │      └─templates
│  │          ├─chat
│  │          ├─course
│  │          ├─group
│  │          ├─matching
└─ └─          └─user

```

## Run Locally

```powershell
mvn spring-boot:run
```

Then open `http://localhost:8080`.

### Windows Java Setup

If Maven reports `release version 17 not supported`, it is using an older JDK. Point `JAVA_HOME` to a JDK 17+ install before running Maven:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn -version
```

`mvn -version` should show Java 17 or newer. To make the setting persistent:

```powershell
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Java\jdk-17", "User")
```

Demo accounts use the password `password`:

| Email | Role in demo |
| --- | --- |
| alice@studylink.test | Group owner and data science learner |
| ben@studylink.test | Backend learner |
| clara@studylink.test | UI/frontend learner |
| devin@studylink.test | Algorithms learner |
| erin@studylink.test | Product-minded learner |

## Useful URLs

- Home: `http://localhost:8080/`
- Dashboard: `http://localhost:8080/dashboard`
- Courses: `http://localhost:8080/courses`
- Study groups: `http://localhost:8080/groups`
- Matches: `http://localhost:8080/matches`
- Health check: `http://localhost:8080/health`
- H2 console: `http://localhost:8080/h2-console`

H2 JDBC URL for the running app: `jdbc:h2:file:./data/studylink`

## Test

```powershell
mvn test
```

## Demo Flow

1. Log in with `alice@studylink.test` / `password`.
2. Review the dashboard and profile fields.
3. Browse courses, open a course, and inspect classmates.
4. Create a study group for a course.
5. Log in as another demo user, request to join the group.
6. Return as the owner and approve the join request.
7. Open matches to see scoring reasons.
8. Enter the group chat and send a message.
9. Show the docs under `docs/` for decisions, diagrams, AI collaboration notes, and the demo script.

## Project Layout

```text
src/main/java/com/example/studylink
  config/
  user/
  course/
  group/
  matching/
  chat/
  common/
src/main/resources/templates
src/test/java/com/example/studylink
docs/
```
