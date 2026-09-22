# Let's Play — RESTful CRUD API

A secure, REST-compliant e-commerce-style backend built with **Spring Boot 4** and **MongoDB**,
featuring JWT authentication, role-based access control (admin vs. user), BCrypt password
hashing, global error handling and CORS.

---

## Table of Contents

- [Let's Play — RESTful CRUD API](#lets-play--restful-crud-api)
  - [Table of Contents](#table-of-contents)
  - [Features](#features)
  - [Tech Stack](#tech-stack)
  - [Prerequisites](#prerequisites)
  - [Project Structure](#project-structure)
  - [Configuration](#configuration)
  - [Running the application](#running-the-application)
  - [API Reference](#api-reference)
    - [Authentication](#authentication)
      - [`POST /api/auth/register` — public](#post-apiauthregister--public)
      - [`POST /api/auth/login` — public](#post-apiauthlogin--public)
    - [Public endpoints](#public-endpoints)
    - [Authenticated endpoints (any logged-in user)](#authenticated-endpoints-any-logged-in-user)
      - [`POST /api/products` — creates a product owned by the caller](#post-apiproducts--creates-a-product-owned-by-the-caller)
      - [`PUT /api/products/{id}` — **owner or ADMIN only**](#put-apiproductsid--owner-or-admin-only)
      - [`DELETE /api/products/{id}` — **owner or ADMIN only**](#delete-apiproductsid--owner-or-admin-only)
    - [Admin endpoints (`ADMIN` role only)](#admin-endpoints-admin-role-only)
  - [Security Design](#security-design)
    - [Authorization matrix](#authorization-matrix)
  - [Error Handling](#error-handling)
  - [Bonus Features](#bonus-features)
  - [Testing](#testing)
- [HTTPS Configuration](#https-configuration)
  - [1. Generate the SSL Certificate](#1-generate-the-ssl-certificate)
    - [What does this command do?](#what-does-this-command-do)
    - [Meaning of each option](#meaning-of-each-option)
  - [2. Understand the Certificate](#2-understand-the-certificate)
  - [3. Put the Keystore in the Project](#3-put-the-keystore-in-the-project)
  - [4. Configure HTTPS in `application.properties`](#4-configure-https-in-applicationproperties)
    - [What does each property do?](#what-does-each-property-do)
      - [`server.port`](#serverport)
      - [`server.ssl.enabled`](#serversslenabled)
      - [`server.ssl.key-store`](#serversslkey-store)
      - [`server.ssl.key-store-password`](#serversslkey-store-password)
      - [`server.ssl.key-store-type`](#serversslkey-store-type)
      - [`server.ssl.key-alias`](#serversslkey-alias)
  - [5. Start the Application](#5-start-the-application)
  - [6. Test HTTPS](#6-test-https)
    - [Important](#important)
  - [7. Test the Register Endpoint](#7-test-the-register-endpoint)
  - [8. HTTP vs HTTPS](#8-http-vs-https)
    - [HTTP](#http)
    - [HTTPS](#https)
  - [9. What Happens Internally?](#9-what-happens-internally)
  - [10. Local Certificate vs Production Certificate](#10-local-certificate-vs-production-certificate)
  - [Summary](#summary)

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





# HTTPS Configuration

This project can run over HTTPS using Spring Boot's embedded Tomcat and a PKCS12 keystore.

> **Note:** The configuration below is intended for local development. The certificate is self-signed, so browsers and clients will not automatically trust it.

---

## 1. Generate the SSL Certificate

Generate a PKCS12 keystore using Java's `keytool`:

```bash
keytool -genkeypair \
  -alias lets-play \
  -keyalg RSA \
  -keysize 2048 \
  -storetype PKCS12 \
  -keystore lets-play.p12 \
  -validity 3650
```

### What does this command do?

It creates a file:

```text
lets-play.p12
```

This file is a **keystore**. It contains the cryptographic material that Spring Boot/Tomcat needs to establish HTTPS connections.

### Meaning of each option

| Option                    | Meaning                                                    |
| ------------------------- | ---------------------------------------------------------- |
| `-genkeypair`             | Generates a public/private key pair and a certificate      |
| `-alias lets-play`        | Name used to identify this certificate inside the keystore |
| `-keyalg RSA`             | Uses RSA for the key pair                                  |
| `-keysize 2048`           | Generates a 2048-bit RSA key                               |
| `-storetype PKCS12`       | Uses the PKCS12 keystore format                            |
| `-keystore lets-play.p12` | Name of the generated keystore file                        |
| `-validity 3650`          | Certificate validity period in days                        |

`3650` days is approximately 10 years.

---

## 2. Understand the Certificate

The generated certificate contains a **public key** and information about the certificate owner.

The keystore also contains the corresponding **private key**.

Conceptually:

```text
                 lets-play.p12
                      |
          +-----------+-----------+
          |                       |
     Private Key             Certificate
          |                       |
       Secret                  Public
          |                       |
          +-----------+-----------+
                      |
                 HTTPS / TLS
```

The **private key must remain secret**.

Do not commit the `.p12` file or its password to Git.

Add the keystore to `.gitignore`:

```gitignore
*.p12
*.jks
```

---

## 3. Put the Keystore in the Project

For local development, put the file inside:

```text
src/main/resources/lets-play.p12
```

The project structure becomes:

```text
lets-play/
├── src/
│   └── main/
│       └── resources/
│           ├── application.properties
│           └── lets-play.p12
├── pom.xml
└── ...
```

Because the file is inside `resources`, Spring Boot can access it through the classpath.

---

## 4. Configure HTTPS in `application.properties`

Add:

```properties
server.port=8443

server.ssl.enabled=true
server.ssl.key-store=classpath:lets-play.p12
server.ssl.key-store-password=YOUR_PASSWORD
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=lets-play
```

### What does each property do?

#### `server.port`

```properties
server.port=8443
```

Changes the port where the HTTPS server listens.

Instead of:

```text
http://localhost:8080
```

the application becomes:

```text
https://localhost:8443
```

`8443` is commonly used for HTTPS during development.

---

#### `server.ssl.enabled`

```properties
server.ssl.enabled=true
```

Tells Spring Boot to enable SSL/TLS for the embedded web server.

Without this:

```text
HTTP
```

With this:

```text
HTTPS
```

---

#### `server.ssl.key-store`

```properties
server.ssl.key-store=classpath:lets-play.p12
```

Tells Spring Boot where the keystore is located.

`classpath:` means:

```text
src/main/resources/
```

So:

```text
classpath:lets-play.p12
```

refers to:

```text
src/main/resources/lets-play.p12
```

---

#### `server.ssl.key-store-password`

```properties
server.ssl.key-store-password=YOUR_PASSWORD
```

This is the password used to open the PKCS12 keystore.

Spring Boot needs it to access the private key and certificate.

Do not hard-code the real password in a public repository.

A better approach is:

```properties
server.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD}
```

Then define:

```bash
export SSL_KEYSTORE_PASSWORD="your-password"
```

---

#### `server.ssl.key-store-type`

```properties
server.ssl.key-store-type=PKCS12
```

Tells Spring Boot the format of the keystore.

This must match:

```bash
-storetype PKCS12
```

from the `keytool` command.

---

#### `server.ssl.key-alias`

```properties
server.ssl.key-alias=lets-play
```

A keystore can contain multiple keys/certificates.

The alias tells Spring Boot which one to use.

It matches:

```bash
-alias lets-play
```

from the `keytool` command.

---

## 5. Start the Application

Run:

```bash
./mvnw spring-boot:run
```

The application should now listen on:

```text
https://localhost:8443
```

You should see something similar to:

```text
Tomcat started on port 8443 (https) 
```

---

## 6. Test HTTPS

Because the certificate is self-signed, `curl` will not trust it by default.

For local testing:

```bash
curl -k https://localhost:8443
```

The `-k` option means:

```text
--insecure
```

It tells `curl` to continue even though the certificate is not signed by a trusted Certificate Authority (CA).

### Important

`-k` should generally be used only for local testing.

Do not use it as a solution for production HTTPS.

---

## 7. Test the Register Endpoint

For example:

```bash
curl -k -X POST https://localhost:8443/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Ahmed",
    "email": "ahmed@example.com",
    "password": "password123"
  }'
```

The request is now sent through an encrypted TLS connection:

```text
Client
   |
   | HTTPS / TLS
   | encrypted
   ↓
Spring Boot
   |
   ↓
/api/auth/register
```

---

## 8. HTTP vs HTTPS

### HTTP

```text
Client
   |
   | HTTP
   ↓
Spring Boot
```

The connection is not encrypted.

### HTTPS

```text
Client
   |
   | HTTPS
   | TLS encryption
   ↓
Spring Boot
```

TLS provides encryption and server authentication based on certificates.

This is especially important when the application sends sensitive information such as:

* Passwords
* JWT tokens
* User information
* Authorization headers

For example:

```http
Authorization: Bearer <JWT>
```

should be transmitted over HTTPS in a real deployment.

---

## 9. What Happens Internally?

When a client connects to:

```text
https://localhost:8443
```

the connection roughly follows this process:

```text
Client
   |
   | 1. TLS connection
   ↓
Tomcat
   |
   | 2. Server presents certificate
   ↓
Client
   |
   | 3. Certificate is verified
   ↓
TLS handshake
   |
   | 4. Secure session established
   ↓
Encrypted HTTP requests
```

Spring Boot passes the SSL configuration to its embedded Tomcat server.

Tomcat uses the private key and certificate from:

```text
lets-play.p12
```

to participate in the TLS handshake.

---

## 10. Local Certificate vs Production Certificate

The certificate generated with `keytool` is **self-signed**.

That means it is useful for:

```text
Development
Local testing
Learning HTTPS
```

but browsers and operating systems do not automatically trust it.

For production, use a certificate issued by a trusted Certificate Authority (CA), such as Let's Encrypt.

A typical production architecture is:

```text
Internet
    |
    | HTTPS :443
    ↓
Reverse Proxy
(Nginx)
    |
    | HTTP/internal HTTPS
    ↓
Spring Boot
    |
    ↓
MongoDB Atlas
```

In that setup, the public HTTPS certificate is usually managed by the reverse proxy.

---

## Summary

The important pieces are:

```text
keytool
   ↓
generates
   ↓
lets-play.p12
   ↓
contains certificate + private key
   ↓
Spring Boot reads it
   ↓
server.ssl.*
   ↓
Tomcat enables TLS
   ↓
https://localhost:8443
```

The main configuration:

```properties
server.port=8443

server.ssl.enabled=true
server.ssl.key-store=classpath:lets-play.p12
server.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD}
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=lets-play
```

And the important security rule:

```text
Private key / keystore password
              ↓
           SECRET
              ↓
      Never commit to Git
```
