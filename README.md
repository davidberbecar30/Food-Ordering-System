# Food Ordering System

Microservices-based food ordering system built with Spring Boot and PostgreSQL, orchestrated with Docker Compose.

## Services

| Service | Port |
|---|---|
| api-gateway | 8080 |
| user-microservice | 8081 |
| menu-microservice | 8082 |
| order-microservice | 8083 |

Each microservice has its own dedicated PostgreSQL database (`user-db`, `menu-db`, `order-db`).

## Instructions

### 1. Create a `.env` file in the root directory

The `.env` file is gitignored — each developer creates their own locally. Use these values for local development:

```
USER_DB_PASSWORD=user_pass
MENU_DB_PASSWORD=menu_pass
ORDER_DB_PASSWORD=order_pass
JWT_SECRET=anything-at-least-32-characters-long
```

### 2. Start your database before working on your microservice

Before running your service locally, start its database container:

```bash
docker compose up user-db

docker compose up menu-db

docker compose up order-db
```

This spins up only the database you need, without starting the other services.
