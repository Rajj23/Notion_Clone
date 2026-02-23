
# Notion Clone

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring%20Boot-4.0-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring%20Security-JWT-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white" />
  <img src="https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white" />
  <img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge" />
</p>

<p align="center">
  A Notion-inspired productivity backend built with <strong>Java & Spring Boot</strong>, featuring <strong>JWT authentication</strong> and designed around real-world backend patterns and scalable architecture.
</p>

---

## Table of Contents

- [Project Status](#project-status)
- [About](#about)
- [Tech Stack](#tech-stack)
- [Authentication API](#authentication-api)
- [Security Architecture](#security-architecture)
- [Getting Started](#getting-started)
- [Roadmap](#roadmap)
- [Contributing](#contributing)

---

## Project Status

| Milestone                           | Status         |
|-------------------------------------|----------------|
| Spring Security + JWT Auth          | **Complete**   |
| Core backend structure              | **Complete**   |
| Hierarchical notes & other features | In Development |

> **Note:** This project is actively being developed. New features and improvements are added regularly.

---

## About

This application aims to recreate key Notion capabilities through a robust backend, currently providing:

- User registration and login with secure JWT-based authentication
- Access & Refresh token mechanism for stateless session management
- Spring Security filter chain with custom JWT filter
- RESTful API design for future client integrations

Frontend and additional backend features are planned — see [Roadmap](#roadmap).

---

## Tech Stack

| Layer       | Technology                            |
|-------------|---------------------------------------|
| Backend     | Java 21, Spring Boot 4               |
| Security    | Spring Security, JWT (jjwt 0.12.6)   |
| Database    | H2 (development), JPA / Hibernate    |
| Build       | Maven                                |
| API         | REST                                 |
| Utilities   | Lombok, ModelMapper, Bean Validation  |

---

## Authentication API

All authentication endpoints are under `/v1/auth/` and require **no token** (publicly accessible).

### `POST` /v1/auth/signup

Register a new user account.

**Request:**
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "yourpassword"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "eyJhbGciOi..."
}
```

---

### `POST` /v1/auth/login

Authenticate an existing user.

**Request:**
```json
{
  "email": "john@example.com",
  "password": "yourpassword"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "eyJhbGciOi..."
}
```

---

### `POST` /v1/auth/refresh

Obtain a new access token using a valid refresh token.

**Request:**
```json
{
  "refreshToken": "eyJhbGciOi..."
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "eyJhbGciOi..."
}
```

---

## Security Architecture

```
Client Request
      │
      ▼
┌─────────────────────┐
│   JwtAuthFilter      │  ◄── Intercepts every request
└────────┬────────────┘
         │
         ▼
┌─────────────────────┐
│  Token Validation    │  ◄── Verifies signature & expiry
└────────┬────────────┘
         │
         ▼
┌─────────────────────┐
│  SecurityContext     │  ◄── Sets authenticated principal
└────────┬────────────┘
         │
         ▼
┌─────────────────────┐
│  Controller Layer    │
└─────────────────────┘
```

| Concept                | Detail                                                                     |
|------------------------|----------------------------------------------------------------------------|
| **Stateless Sessions** | No server-side session storage; each request is authenticated via JWT      |
| **Access Token**       | Short-lived (15 min), carries user identity and claims                     |
| **Refresh Token**      | Long-lived (7 days), stored hashed in DB, supports token rotation          |
| **Signing Algorithm**  | HMAC-SHA512 for token integrity                                            |
| **Password Storage**   | BCrypt hashing via Spring `PasswordEncoder`                                |
| **Filter Chain**       | Custom `JwtAuthFilter` runs before `UsernamePasswordAuthenticationFilter`  |

---

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.8+

### Installation

```bash
# Clone the repository
git clone https://github.com/Rajj23/Notion_Clone.git
cd Notion_Clone
```

### Configuration

The app uses **H2 in-memory database** by default — no external DB setup required for development.

Update `src/main/resources/application.properties` if you need to change JWT secret or token validity:

```properties
jwt.secret=your-secret-key
jwt.accessTokenValidity=900000
jwt.refreshTokenValidity=604800000
```

### Run

```bash
mvn clean install
mvn spring-boot:run
```

### Test the API

Use [Postman](https://www.postman.com/) or any HTTP client to call the authentication endpoints listed above.

---

## Roadmap

- [x] JWT Authentication (Access + Refresh tokens)
- [x] User Signup & Login
- [x] Stateless Spring Security configuration
- [x] Request validation with Bean Validation
- [ ] Hierarchical notes — CRUD operations
- [ ] Shareable notes
- [ ] Tagging & search functionality
- [ ] Frontend UI

---

## Contributing

Contributions are welcome. To get started:

1. Fork the repository
2. Create a feature branch
   ```bash
   git checkout -b feature/your-feature
   ```
3. Commit your changes
4. Push to your fork and open a **Pull Request**

Please ensure your code follows the existing project conventions.

---

## Why This Project

This is more than a clone — it's a structured exercise in backend engineering:

- Applying Java fundamentals to real application architecture
- Implementing JWT and Spring Security following industry best practices
- Building scalable RESTful APIs with clean separation of concerns
- Laying groundwork for extensible, production-ready design

---

<p align="center">
  If you find this useful, consider giving the repo a <strong>star</strong> and following along on <a href="https://github.com/Rajj23">GitHub</a>.
</p>
