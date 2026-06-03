# Decision Log

| Date | Decision | Reason | Tradeoff |
| --- | --- | --- | --- |
| 2026-05-31 | Use Spring Boot 3 with Java 17 | Matches Spring Boot 3's baseline while keeping the project easier to run on common local JDK installations. | Less lightweight than a tiny servlet app, but much faster to build and test safely. |
| 2026-05-31 | Use Thymeleaf and Bootstrap 5 | Server-rendered pages keep the demo simple and avoid a separate frontend build. Bootstrap gives familiar responsive UI primitives. | Interactions are less rich than a SPA, but the MVP remains easy to inspect. |
| 2026-05-31 | Use H2 for development/demo data | H2 runs without external services and supports the H2 console for demos. | Production would need a durable database such as PostgreSQL. |
| 2026-05-31 | Keep business rules in services | Controllers stay thin, and tests can cover duplicate enrollment, join approvals, matching, and chat access. | Services need explicit authorization checks instead of relying on controller routing alone. |
| 2026-05-31 | Use Spring Security form login with BCrypt | Provides standard authentication, session handling, logout, and password hashing. | OAuth/SSO is out of MVP scope. |
| 2026-05-31 | Use rule-based matching first | The scoring formula is transparent, testable, and aligned with the brief. | It is not personalized by historical behavior yet. |
| 2026-05-31 | Use WebSocket/STOMP for chat while keeping HTTP fallback | STOMP topics match the required endpoint shape, and HTTP post keeps the feature demo-friendly if live scripts are unavailable. | True multi-device real-time UX still needs stronger browser-side reconnect handling. |
