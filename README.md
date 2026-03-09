# BlockVerse — Backend API

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk&logoColor=white" alt="Java 21" />
  <img src="https://img.shields.io/badge/Spring%20Boot-4.0-6DB33F?style=flat-square&logo=springboot&logoColor=white" alt="Spring Boot 4" />
  <img src="https://img.shields.io/badge/Security-JWT-6DB33F?style=flat-square&logo=springsecurity&logoColor=white" alt="JWT" />
  <img src="https://img.shields.io/badge/Build-Maven-C71A36?style=flat-square&logo=apachemaven&logoColor=white" alt="Maven" />
  <img src="https://img.shields.io/badge/Tests-JUnit%205-25A162?style=flat-square&logo=junit5&logoColor=white" alt="JUnit 5" />
  <img src="https://img.shields.io/badge/License-MIT-blue?style=flat-square" alt="MIT" />
</p>

REST API backend for a collaborative block-based workspace application. Supports JWT authentication, workspace management, role-based access control, document CRUD, and hierarchical block operations — built with Spring Boot 4 and Java 21.

---

## Quick Start

```bash
git clone https://github.com/Rajj23/BlockVerse.git
cd BlockVerse
mvn clean install
mvn spring-boot:run
```

Uses H2 in-memory database by default. No external setup required.

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

| Concern    | Implementation                                                              |
|------------|-----------------------------------------------------------------------------|
| Auth       | Stateless JWT (access + refresh token rotation), BCrypt password hashing    |
| Validation | Jakarta Bean Validation on all request DTOs                                 |
| Errors     | Global `@RestControllerAdvice` with a consistent error response shape       |
| Tests      | `@WebMvcTest` controller tests + `@ExtendWith(MockitoExtension)` unit tests |

---

## API

Base URL: `http://localhost:8080`

### Auth `/v1/auth` — public

| Method | Endpoint   | Body                          | Returns                          |
|--------|------------|-------------------------------|----------------------------------|
| POST   | `/signup`  | `{ name, email, password }`   | `{ accessToken, refreshToken }` |
| POST   | `/login`   | `{ email, password }`         | `{ accessToken, refreshToken }` |
| POST   | `/refresh` | `{ refreshToken }`            | `{ accessToken, refreshToken }` |

### Workspaces `/v1/workspaces` — authenticated

| Method | Endpoint        | Body / Params           | Returns                     |
|--------|-----------------|-------------------------|-----------------------------|
| POST   | `/create`       | `{ name, workSpaceType }` | `"WorkSpace created…"`    |
| GET    | `/all`          | —                       | `WorkSpaceDetailsResponse[]` |
| GET    | `/{id}`         | —                       | `WorkSpaceDetailsResponse`  |
| PUT    | `/update/{id}`  | `{ name, workSpaceType }` | `"WorkSpace updated…"`    |
| DELETE | `/delete/{id}`  | —                       | `"WorkSpace deleted…"`      |

Types: `PRIVATE`, `TEAM`

### Members `/v1/workspace/member/{workspaceId}` — authenticated, RBAC

| Method | Endpoint                             | Body / Params         | Access        |
|--------|--------------------------------------|-----------------------|---------------|
| POST   | `/add`                               | `{ email, role }`     | OWNER, ADMIN  |
| DELETE | `/remove?email=`                     | query param           | OWNER, ADMIN  |
| POST   | `/change-role`                       | `{ email, role }`     | OWNER, ADMIN  |
| POST   | `/leave`                             | —                     | ADMIN, MEMBER |
| POST   | `/transfer-ownership?newOwnerEmail=` | query param           | OWNER only    |
| GET    | `/count-members`                     | —                     | Any member    |

Roles: `OWNER` / `ADMIN` / `MEMBER`

#### Permission Matrix

| Action             | OWNER | ADMIN  | MEMBER |
|--------------------|:-----:|:------:|:------:|
| Add member         |  Yes  |  Yes   |  No    |
| Remove member      |  Yes  |  Yes*  |  No    |
| Assign OWNER role  |  Yes  |  No    |  No    |
| Change role        |  Yes  |  Yes   |  No    |
| Transfer ownership |  Yes  |  No    |  No    |
| Delete workspace   |  Yes  |  No    |  No    |
| Leave workspace    |  No   |  Yes   |  Yes   |

*Admins cannot remove other admins or the owner.*

### Documents `/v1/documents` — authenticated, workspace-scoped

| Method | Endpoint                        | Body / Params        | Access         |
|--------|---------------------------------|----------------------|----------------|
| POST   | `/{workspaceId}`                | `{ title }`          | Any member     |
| GET    | `/{documentId}`                 | —                    | Any member     |
| GET    | `/{documentId}/blocks`          | —                    | Any member     |
| GET    | `/workspace/{workspaceId}`      | —                    | Any member     |
| PUT    | `/{documentId}`                 | `{ title }`          | Any member     |
| POST   | `/{documentId}/archive`         | —                    | OWNER, ADMIN   |
| POST   | `/{documentId}/unarchive`       | —                    | OWNER, ADMIN   |

### Blocks `/v1/blocks` — authenticated, workspace-scoped

| Method | Endpoint                  | Body                                              | Notes                        |
|--------|---------------------------|---------------------------------------------------|------------------------------|
| POST   | `/{documentId}`           | `{ parentId?, type, content, documentVersion? }`  | Creates block at end of list |
| PUT    | `/{blockId}`              | `{ type, content, documentVersion? }`             | Updates content and type     |
| DELETE | `/{blockId}`              | `{ documentVersion? }`                            | Soft delete                  |
| PUT    | `/{blockId}/move`         | `{ newParentId?, newPosition, documentVersion? }` | Reparent or reorder          |
| GET    | `/{blockId}/children`     | —                                                 | Returns direct children      |

Block types: `PARAGRAPH`, `HEADING1`, `HEADING2`, `HEADING3`, `BULLET`, `NUMBERED`, `QUOTE`, `CODE`

Positions use a `BigInteger` gap strategy (increments of 10,000) to allow insertion without reordering. All mutations record a `BlockChangeLog` entry and enforce optimistic concurrency via `documentVersion`.

---

## Error Response

```json
{
  "status": 403,
  "message": "Forbidden",
  "error": "Only workspace owners and admin can add members",
  "timestamp": "2026-03-09T10:00:00"
}
```

| Status | Condition                                              |
|--------|--------------------------------------------------------|
| 400    | Validation failure, duplicate member, owner leave, block level constraint |
| 401    | Invalid or expired token                               |
| 403    | Insufficient permission, not a workspace member        |
| 404    | User, workspace, document, or block not found          |
| 409    | Email already registered, document version conflict    |

---

## Testing

Controller tests use `@WebMvcTest` with security filters disabled and all dependencies mocked via `@MockitoBean`.  
Service tests use `@ExtendWith(MockitoExtension.class)` with all repositories and utilities mocked.

```bash
mvn test
```

| Suite                            | Coverage                                                            |
|----------------------------------|---------------------------------------------------------------------|
| `AuthControllerTest`             | Login, signup, validation                                           |
| `WorkspaceControllerTest`        | CRUD, permission errors, input validation                           |
| `WorkspaceMemberControllerTest`  | All 6 endpoints, permission matrix, validation                      |
| `BlockControllerTest`            | Create, update, delete, move, get children — success and error paths |
| `DocumentControllerTest`         | Create, get, list, update, archive, unarchive — full permission coverage |
| `AuthServiceTest`                | Token generation, signup, login, refresh token flow, error cases    |
| `WorkspaceServiceTest`           | Create, update, delete, ownership transfer, member listing          |
| `WorkspaceMemberServiceTest`     | Add, remove, role change, leave, transfer ownership, member count   |
| `BlockServiceTest`               | Create (root/child/position/conflicts), update, delete, move, get children |
| `DocumentServiceTest`            | Create, get, list, update, archive, unarchive — all role combinations |

---

## Project Structure

```
src/main/java/com/blockverse/app/
├── config/        # AppConfig — AuthenticationManager, PasswordEncoder, ModelMapper beans
├── controller/    # AuthController, WorkSpaceController, WorkSpaceMemberController,
│                  # DocumentController, BlockController
├── dto/           # Request / response DTOs with Bean Validation
├── entity/        # User, WorkSpace, WorkSpaceMember, Document, Block, BlockChangeLog
├── enums/         # WorkSpaceRole, WorkSpaceType, BlockType, BlockOperationType
├── exception/     # Custom exceptions + GlobalExceptionHandler
├── mapper/        # BlockMapper, DocumentMapper
├── repo/          # Spring Data JPA repositories
├── security/      # JwtUtil, JwtAuthFilter, AuthService, CustomUserDetailService,
│                  # SecurityUtil, WebSecurityConfig
└── service/       # WorkSpaceService, WorkSpaceMemberService, DocumentService, BlockService
```

---

## Roadmap

- [x] JWT auth with refresh token rotation
- [x] Workspace CRUD with ownership enforcement
- [x] Role-based member management (OWNER / ADMIN / MEMBER)
- [x] Global exception handling
- [x] Bean Validation on all endpoints
- [x] Document CRUD with archive/unarchive
- [x] Hierarchical block model with position gap strategy
- [x] Optimistic concurrency via document versioning
- [x] Block change log
- [x] Controller and service unit tests (full coverage)
- [ ] Page sharing and granular per-document permissions
- [ ] Full-text search
- [ ] WebSocket real-time collaboration
- [ ] Frontend

---

## License

MIT
