# Notion Clone — Backend API

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk&logoColor=white" alt="Java 21" />
  <img src="https://img.shields.io/badge/Spring%20Boot-4.0-6DB33F?style=flat-square&logo=springboot&logoColor=white" alt="Spring Boot 4" />
  <img src="https://img.shields.io/badge/Security-JWT-6DB33F?style=flat-square&logo=springsecurity&logoColor=white" alt="JWT" />
  <img src="https://img.shields.io/badge/Build-Maven-C71A36?style=flat-square&logo=apachemaven&logoColor=white" alt="Maven" />
  <img src="https://img.shields.io/badge/Tests-JUnit%205-25A162?style=flat-square&logo=junit5&logoColor=white" alt="JUnit 5" />
  <img src="https://img.shields.io/badge/License-MIT-blue?style=flat-square" alt="MIT" />
</p>

REST API backend for a Notion-style workspace app. JWT auth, workspace CRUD, role-based member management — built with Spring Boot 4 and Java 21.

---

## Quick Start

```bash
git clone https://github.com/Rajj23/Notion_Clone.git
cd Notion_Clone
mvn clean install
mvn spring-boot:run
```

Uses H2 in-memory DB by default. No external setup required.

```properties
# src/main/resources/application.properties
jwt.secret=your-secret-key
jwt.accessTokenValidity=900000        # 15 min
jwt.refreshTokenValidity=604800000    # 7 days
```

---

## Architecture

```
Client → JwtAuthFilter → SecurityContext → Controller → Service → Repository → H2
```

| Concern    | Implementation                                  |
|------------|--------------------------------------------------|
| Auth       | Stateless JWT (access + refresh), BCrypt passwords |
| Validation | Jakarta Bean Validation on all request DTOs       |
| Errors     | Global `@RestControllerAdvice` with consistent response shape |
| Tests      | `@WebMvcTest` controller tests + `@MockitoBean` service tests |

---

## API

Base URL: `http://localhost:8080`

### Auth `/v1/auth` — public

| Method | Endpoint   | Body                                          | Returns              |
|--------|------------|-----------------------------------------------|----------------------|
| POST   | `/signup`  | `{ name, email, password }`                   | `{ accessToken, refreshToken }` |
| POST   | `/login`   | `{ email, password }`                         | `{ accessToken, refreshToken }` |
| POST   | `/refresh` | `{ refreshToken }`                            | `{ accessToken, refreshToken }` |

### Workspaces `/v1/workspaces` — authenticated

| Method | Endpoint        | Body / Params                    | Returns                |
|--------|-----------------|----------------------------------|------------------------|
| POST   | `/create`       | `{ name, workSpaceType }`        | `"Workspace created…"` |
| GET    | `/all`          | —                                | `WorkSpaceDetailsResponse[]` |
| GET    | `/{id}`         | —                                | `WorkSpaceDetailsResponse` |
| PUT    | `/update/{id}`  | `{ name, workSpaceType }`        | `"WorkSpace updated…"` |
| DELETE | `/delete/{id}`  | —                                | `"WorkSpace deleted…"` |

Types: `PRIVATE`, `TEAM`

### Members `/v1/workspace/member/{workspaceId}` — authenticated, RBAC

| Method | Endpoint                | Body / Params            | Access       |
|--------|-------------------------|--------------------------|--------------|
| POST   | `/add`                  | `{ email, role }`        | OWNER, ADMIN |
| DELETE | `/remove?email=`        | query param              | OWNER, ADMIN |
| POST   | `/change-role`          | `{ email, role }`        | OWNER, ADMIN |
| POST   | `/leave`                | —                        | ADMIN, MEMBER|
| POST   | `/transfer-ownership?newOwnerEmail=` | query param | OWNER only   |
| GET    | `/count-members`        | —                        | Any member   |

Roles: `OWNER` · `ADMIN` · `MEMBER`

#### Permission Matrix

| Action             | OWNER | ADMIN | MEMBER |
|--------------------|:-----:|:-----:|:------:|
| Add member         |  Y    |  Y    |   –    |
| Remove member      |  Y    |  Y*   |   –    |
| Assign OWNER role  |  Y    |  –    |   –    |
| Change role        |  Y    |  Y    |   –    |
| Transfer ownership |  Y    |  –    |   –    |
| Delete workspace   |  Y    |  –    |   –    |
| Leave workspace    |  –    |  Y    |   Y    |

*\*Admins cannot remove other admins.*

---

## Error Response

```json
{
  "status": 403,
  "message": "Forbidden",
  "error": "Only workspace owners and admin can add members",
  "timestamp": "2026-02-26T10:00:00"
}
```

| Status | When                                              |
|--------|---------------------------------------------------|
| 400    | Validation failure, duplicate member, owner leave  |
| 401    | Invalid / expired token                            |
| 403    | Insufficient permission, not a workspace member    |
| 404    | User or workspace not found                        |
| 409    | Email already registered                           |

---

## Testing

Controller-layer tests use `@WebMvcTest` with mocked services.  
Service-layer tests use `@ExtendWith(MockitoExtension.class)` with mocked repositories.

```bash
mvn test
```

| Suite                            | Coverage                                    |
|----------------------------------|---------------------------------------------|
| `AuthControllerTest`             | Login, signup, validation                   |
| `WorkspaceControllerTest`        | CRUD, permission errors, input validation   |
| `WorkspaceMemberControllerTest`  | All 6 endpoints, permission matrix, validation |
| `AuthServiceTest`                | Token generation, refresh, error cases      |
| `WorkspaceServiceTest`           | Create, update, delete, ownership checks    |
| `WorkspaceMemberServiceTest`     | Add/remove/role-change/leave/transfer logic |

---

## Project Structure

```
src/main/java/com/notion/demo/
├── config/        # AppConfig, beans
├── controller/    # AuthController, WorkSpaceController, WorkSpaceMemberController
├── dto/           # Request/response DTOs with Bean Validation
├── entity/        # User, WorkSpace, WorkSpaceMember (JPA)
├── enums/         # WorkSpaceRole, WorkSpaceType
├── exception/     # Custom exceptions + GlobalExceptionHandler
├── repo/          # Spring Data JPA repositories
├── security/      # JwtUtil, JwtAuthFilter, AuthService, WebSecurityConfig
└── service/       # WorkSpaceService, WorkSpaceMemberService
```

---

## Roadmap

- [x] JWT auth with refresh token rotation
- [x] Workspace CRUD with ownership enforcement
- [x] Role-based member management (OWNER / ADMIN / MEMBER)
- [x] Global exception handling
- [x] Bean Validation on all endpoints
- [x] Controller + service unit tests
- [ ] Pages / notes — hierarchical CRUD
- [ ] Page sharing & granular permissions
- [ ] Full-text search
- [ ] Frontend

---

## License

MIT
