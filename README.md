# Let's Play — RESTful CRUD API

A secure, REST-compliant e-commerce-style backend built with **Spring Boot 4** and **MongoDB**,
featuring JWT authentication, role-based access control (admin vs. user), BCrypt password
hashing, global error handling and CORS.

---

## Table of Contents

1. [Features](#features)
2. [Tech Stack](#tech-stack)
3. [Prerequisites](#prerequisites)
4. [Project Structure](#project-structure)
5. [Configuration](#configuration)
6. [Running the application](#running-the-application)
7. [API Reference](#api-reference)
   - [Authentication](#authentication)
   - [Public endpoints](#public-endpoints)
   - [Authenticated endpoints](#authenticated-endpoints)
   - [Admin endpoints](#admin-endpoints)
8. [Security Design](#security-design)
9. [Error Handling](#error-handling)
10. [Bonus Features](#bonus-features)
11. [Testing](#testing)

---

## Features

- **Users & Products** — two entities with a one-to-many relationship (a user owns many products; each product belongs to one user via `userId`).
- **JWT authentication** — register, log in, receive a signed token, then use it as `Authorization: Bearer <token>`.
- **Role-based access control**:
  - `ADMIN` — manage all users and all products.
  - `USER` — manage only their own products.
- **Public product catalog** — `GET /api/products` and `GET /api/products/{id}` require no token.
- **Secure password handling** — BCrypt hashing + salting; passwords are **never** serialized in responses or logs.
- **Input validation** — Jakarta Bean Validation on every request body (clear 400 responses).
- **Global exception handling** — consistent JSON error bodies, never an unhandled 5XX.
- **MongoDB injection resilience** — data access goes through Spring Data repositories (parameterized queries), not string-built queries.
- **Bonus: CORS** — fine-grained policy for `http://localhost:4200`.

## Tech Stack

| Layer      | Technology                                              |
| ---------- | ------------------------------------------------------- |
| Framework  | Spring Boot 4.1 (Spring MVC, Spring Security)           |
| Database   | MongoDB (Atlas in dev, via `spring-boot-starter-data-mongodb`) |
| Auth       | JWT (`jjwt` 0.11.5), BCrypt                              |
| Validation | Jakarta Bean Validation (`spring-boot-starter-validation`) |
| JSON       | Jackson 3 (included with `spring-boot-starter-webmvc`)   |
| Build      | Maven, Java 17+                                          |

## Prerequisites

- JDK **17 or newer** (developed on 21)
- Maven (the included `./mvnw` wrapper works without a local install)
- A MongoDB instance — connection string in `.env`

## Project Structure

```
src/main/java/letsPlay/
├── config/
│   ├── CustomUserDetailsService.java   # loads users from the DB for Spring Security
│   ├── JwtFilter.java                  # validates Bearer tokens, throws GlobalException on bad tokens
│   ├── JwtUtil.java                    # token generation / parsing
│   ├── MakeAdmin.java                  # seeds an admin account (admin / admin123)
│   ├── RestAccessDeniedHandler.java    # throws 403 on missing role
│   ├── RestAuthenticationEntryPoint.java # throws 401 on missing/invalid token
│   └── SecurityConfig.java             # filter chain, authorization rules, CORS, BCrypt
├── controller/
│   ├── AdminUserController.java        # /api/admin/users (ADMIN only)
│   ├── AuthController.java             # /api/auth/register, /api/auth/login
│   └── ProductController.java          # /api/products CRUD
├── dto/                                # request/response records
├── enums/Role.java                     # ADMIN, USER
├── exception/                          # GlobalException, GlobalExceptionHandler, ApiErrorController
├── models/                             # UserModel, ProductModel (Mongo documents)
├── repository/                         # Spring Data MongoRepository interfaces
└── service/                            # AuthService, ProductService, UserService
```

## Configuration

Settings come from `.env` (already gitignored) and `application.properties`:

| Property | Description |
| -------- | ----------- |
| `MONGODB_URI` | MongoDB connection string |
| `APPLICATION_SECURITY_JWT_SECRET_KEY` | HMAC-SHA256 key used to sign JWTs (≥ 32 bytes) |
| `APPLICATION_SECURITY_JWT_EXPIRATION` | Token lifetime in ms (default 86400000 = 24 h) |
| `spring.security.filter.dispatcher-types` | Dispatcher types the security chain runs on; excludes ERROR so errors thrown in the chain are forwarded to `/error` once |

> **HTTPS:** in production the API must be served over HTTPS (TLS terminates at a reverse
> proxy / load balancer, or via `server.port` + `server.ssl.*` properties). JWTs and
> passwords must never travel over plain HTTP.

## Running the application

```bash
make backend          # or: ./mvnw spring-boot:run
```

On first start the `MakeAdmin` seeder creates:

```
username: admin
email:    admin@gmail.com
password: admin123
role:     ADMIN
```

The embedded Tomcat serves the API on `http://localhost:8080`.

## API Reference

All requests and responses are **JSON** (`Content-Type: application/json`).
Protected endpoints expect the header `Authorization: Bearer <token>`.

### Authentication

#### `POST /api/auth/register` — public

Creates a normal `USER` account and returns a JWT + profile.

```json
{ "name": "jane", "email": "jane@example.com", "password": "secret123" }
```

**201 Created**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "expiresAt": "2026-09-20T10:00:00",
  "user": { "id": "...", "name": "jane", "email": "jane@example.com", "role": "USER" }
}
```

**409 Conflict** — username or email already taken. **400** — validation failed.

#### `POST /api/auth/login` — public

Accepts **username or email** (whichever is supplied) + password.

```json
{ "name": "jane", "password": "secret123" }
```

**200 OK** — same response shape as register. **401** — invalid credentials.

### Public endpoints

| Method | Path                 | Description            |
| ------ | -------------------- | ---------------------- |
| `GET`  | `/api/products`      | List all products      |
| `GET`  | `/api/products/{id}` | Get a single product   |

Product JSON shape:

```json
{
  "id": "...",
  "name": "Wireless Mouse",
  "price": 29.99,
  "description": "Ergonomic 2.4GHz mouse",
  "userId": "<owner id>"
}
```

### Authenticated endpoints (any logged-in user)

#### `POST /api/products` — creates a product owned by the caller

```json
{ "name": "Wireless Mouse", "price": 29.99, "description": "Ergonomic 2.4GHz mouse" }
```

**201 Created** → full product object. **400** — validation failed. **401** — no/invalid token.

#### `PUT /api/products/{id}` — **owner or ADMIN only**

Replaces the product. Same JSON body as POST. **200 OK**, **403** if you are not the owner and not an admin, **404** if the product does not exist.

#### `DELETE /api/products/{id}` — **owner or ADMIN only**

**204 No Content** on success, **403** / **404** otherwise.

### Admin endpoints (`ADMIN` role only)

All of these return **401** for anonymous calls and **403** for non-admin users.
Passwords are excluded from every response.

| Method   | Path                   | Description                                  |
| -------- | ---------------------- | -------------------------------------------- |
| `GET`    | `/api/admin/users`     | List all users                               |
| `GET`    | `/api/admin/users/{id}`| Get one user                                 |
| `PUT`    | `/api/admin/users/{id}`| Update name / email / password / role        |
| `DELETE` | `/api/admin/users/{id}`| Delete a user **and all of their products**  |

`PUT /api/admin/users/{id}` example — all fields optional:

```json
{ "name": "jane", "email": "jane@new.com", "password": "newpass123", "role": "ADMIN" }
```

Notes:
- Cannot assign an invalid role (400).
- Cannot delete your own account (400).
- Duplicate name/email on update → 409.

## Security Design

| Concern | Implementation |
| ------- | -------------- |
| Password storage | `BCryptPasswordEncoder` (hash + salt) before persisting |
| Password exposure | `@JsonIgnore` + `WRITE_ONLY` on the entity — never serialized |
| Token handling | Stateless JWT (HS256), 24 h expiry, validated by `JwtFilter` on every request |
| Authorization | Rule table in `SecurityConfig` + ownership checks inside `ProductService` |
| Injection | All queries via Spring Data repositories (parameterized), never string-built MongoDB queries |
| Transport | JWT secrets from `.env`; HTTPS required in production |

### Authorization matrix

| Endpoint                             | Anonymous | USER        | ADMIN       |
| ------------------------------------ | --------- | ----------- | ----------- |
| `POST /api/auth/register`            | ✅        | ✅          | ✅          |
| `POST /api/auth/login`               | ✅        | ✅          | ✅          |
| `GET /api/products` + `/{id}`        | ✅        | ✅          | ✅          |
| `POST /api/products`                 | ❌ 401    | ✅          | ✅          |
| `PUT/DELETE /api/products/{id}`      | ❌ 401    | owner only (else 403) | ✅ |
| `/api/admin/users/**`                | ❌ 401    | ❌ 403      | ✅          |

## Error Handling

`GlobalExceptionHandler` (`@RestControllerAdvice`) converts every exception into:

```json
{
  "timestamp": "2026-09-19T12:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Product not found",
  "path": "/api/products/abc",
  "validationErrors": null
}
```

| Status | Used for |
| ------ | -------- |
| 400 | Validation failures, malformed JSON, bad path variables / parameters, self-delete |
| 401 | Missing/invalid/expired token, wrong credentials |
| 403 | Insufficient role, modifying someone else's product |
| 404 | Unknown product/user/route |
| 405 | Wrong HTTP method on a resource |
| 409 | Duplicate username/email, conflicting state |

Exceptions thrown **inside Spring MVC** (controllers, services) are converted by
`GlobalExceptionHandler` (`@RestControllerAdvice`). Exceptions thrown **earlier,
in the security filter chain** — the JWT filter, the authentication entry point and
the access-denied handler all `throw new GlobalException(...)` — are re-dispatched by
the servlet container to `/error`, where `ApiErrorController` rebuilds the original
`GlobalException` (status + message) and renders it with the exact same body shape.
The security chain is configured with `spring.security.filter.dispatcher-types=async, request`
so it does not re-run on the error re-dispatch (which would otherwise loop). A
last-resort handler/controller guarantees no unhandled 5XX responses. Invalid/unknown
routes return JSON 404 instead of whitelabel — note that *unauthenticated* requests to
unknown **protected** paths are rejected with **401** by the security layer before
routing ever happens (with a valid token they get 404).

## Bonus Features

- **CORS** — `SecurityConfig` whitelists `http://localhost:4200` with explicit methods/headers and credentials.

## Testing

Smoke test with curl:

```bash
# register + login
curl -s -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"name":"jane","email":"jane@example.com","password":"secret123"}'

# login as admin (seeded)
curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"name":"admin","password":"admin123"}'

# public product list
curl -s localhost:8080/api/products

# create a product (use a token)
curl -s -X POST localhost:8080/api/products -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <token>' \
  -d '{"name":"Wireless Mouse","price":29.99,"description":"Ergonomic mouse"}'
```