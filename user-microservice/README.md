# User Service

The authentication and user management microservice of the **Food Ordering System** project.

Responsibilities:
- User registration (with validation + BCrypt-hashed password)
- Login → issues an HS256-signed JWT
- Own profile management (`GET /me`, `PUT /me`)
- Listing users (ADMIN only)
- Internal endpoints for the other microservices (user existence check, token validation)

## Stack

- Java 17, Spring Boot 3.5.4
- Spring Web, Spring Data JPA, Spring Security
- PostgreSQL (`user_db`)
- JWT (jjwt 0.12)
- BCrypt (strength 12)
- springdoc-openapi (Swagger UI)
- Maven
- JUnit 5 + Mockito + Spring Security Test (unit tests)
- Docker

## N-Layers Architecture

```
com.foodordering.userservice
├── UserServiceApplication       # Main
├── config/                      # SecurityConfig, OpenApiConfig
├── controller/                  # REST endpoints
├── service/                     # Business logic (interface + impl)
├── repository/                  # Spring Data JPA
├── entity/                      # JPA entities + enums
├── dto/
│   ├── request/                 # Request body DTOs
│   └── response/                # Response body DTOs
├── mapper/                      # Entity <-> DTO
├── exception/                   # Exceptions + GlobalExceptionHandler
└── security/                    # JWT, filters, UserDetails
```

## How to Run

### 1. With a local PostgreSQL

```bash
# start a Postgres instance
docker run --name user-db -e POSTGRES_DB=user_db \
  -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 -d postgres:16

# build + run
./mvnw spring-boot:run
```

The application starts on `http://localhost:8081`.

### 2. Environment variables (all optional, with dev defaults)

| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/user_db` | JDBC URL |
| `DB_USERNAME` | `postgres` | DB user |
| `DB_PASSWORD` | `postgres` | DB password |
| `SERVER_PORT` | `8081` | HTTP port |
| `APP_JWT_SECRET` | (dev placeholder) | JWT secret, at least 32 bytes |
| `APP_JWT_EXPIRATION_MS` | `86400000` (24h) | Token lifetime |

In production, `APP_JWT_SECRET` **must** be overridden.

### 3. With Docker

```bash
docker build -t food-ordering/user-service .
docker run --rm -p 8081:8081 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/user_db \
  -e APP_JWT_SECRET="$(openssl rand -base64 48)" \
  food-ordering/user-service
```

## Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/users/register` | Public | Register a new user (creates a CUSTOMER) |
| POST | `/api/users/login` | Public | Login, returns a JWT |
| GET | `/api/users/me` | CUSTOMER \| ADMIN | Own profile |
| PUT | `/api/users/me` | CUSTOMER \| ADMIN | Update own profile |
| GET | `/api/users` | ADMIN | List all users |
| GET | `/api/users/{id}/exists` | Internal | Does the user exist? |
| GET | `/api/users/{id}/validate-token` | Internal | Validates the JWT from the header |
| POST | `/api/admin/users` | ADMIN | Create a user with any role (CUSTOMER or ADMIN) |
| GET | `/api/admin/users` | ADMIN | List users, optionally filtered with `?role=ADMIN` or `?role=CUSTOMER` |
| GET | `/api/admin/users/{id}` | ADMIN | Read any user by id |
| PUT | `/api/admin/users/{id}` | ADMIN | Update any user (username, email, password, role) |
| DELETE | `/api/admin/users/{id}` | ADMIN | Delete a user (an admin cannot delete their own account) |

**Swagger UI:** `http://localhost:8081/swagger-ui.html`

## Default Admin Account

On startup, if no ADMIN exists in the database, `AdminInitializer` creates a default one
(otherwise there would be no way to ever obtain an admin, since `/register` always forces CUSTOMER):

| Variable | Default |
|---|---|
| `APP_ADMIN_USERNAME` | `admin` |
| `APP_ADMIN_EMAIL` | `admin@foodordering.local` |
| `APP_ADMIN_PASSWORD` | `admin123` |

In production, these variables **must** be overridden.

## curl Examples

### Register
```bash
curl -X POST http://localhost:8081/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","email":"alice@example.com","password":"secret123"}'
```

### Login
```bash
TOKEN=$(curl -s -X POST http://localhost:8081/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"secret123"}' | jq -r .token)
```

### Me
```bash
curl http://localhost:8081/api/users/me -H "Authorization: Bearer $TOKEN"
```

### Admin: login as admin + user CRUD
```bash
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8081/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r .token)

# create a new ADMIN
curl -X POST http://localhost:8081/api/admin/users \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"username":"admin2","email":"admin2@example.com","password":"secret123","role":"ADMIN"}'

# list only the admins
curl "http://localhost:8081/api/admin/users?role=ADMIN" -H "Authorization: Bearer $ADMIN_TOKEN"

# promote a CUSTOMER to ADMIN
curl -X PUT http://localhost:8081/api/admin/users/<id> \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"role":"ADMIN"}'

# delete a user
curl -X DELETE http://localhost:8081/api/admin/users/<id> \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

## Security

- **BCrypt** strength 12 for passwords.
- **JWT HS256**, signed with a secret from the environment. Claims: `sub` (userId UUID), `username`, `role`.
- All endpoints are `SessionCreationPolicy.STATELESS`.
- `JwtAuthenticationFilter` extracts the token from `Authorization: Bearer <token>` and populates the `SecurityContext` with a `UserPrincipal`.
- Role-based restrictions via `@PreAuthorize("hasRole('ADMIN')")` in the controllers.
- `/exists` and `/validate-token` are open on the cluster's internal network — in K8s they are isolated via NetworkPolicy. (For stricter security, you can add an `X-Internal-Key` header and a dedicated filter.)
- The password is **never** returned — `UserResponse` does not contain it.

## Notes on Integration with the Other Services

Order Service and Menu Service receive requests through the Gateway, with the same JWT. They have two options:

1. **Decode the JWT locally** (recommended) — they share the same `APP_JWT_SECRET` and validate the token without a network call.
2. **Call** `GET /api/users/{id}/validate-token` — safer, but adds an HTTP request to every flow.

The `/exists` endpoint is useful for Menu Service when validating `manager_id`.

## Tests

```bash
./mvnw test
```

Contains unit tests for:
- `UserServiceImpl` (business logic)
- `JwtService` (token generation + validation)
- `UserPrincipal`
- `UserController` (with MockMvc + Spring Security Test)