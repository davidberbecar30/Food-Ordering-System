# Food Ordering System

Microservices-based food ordering system built with Spring Boot and PostgreSQL. Each service has its own dedicated database and communicates over HTTP. JWT tokens are issued by the User Service and validated by the Order Service on every request.

## Architecture

```
Client
  │
  ▼
API Gateway  :8080   (routes /users/**, /menu/**, /orders/**)
  │
  ├── User Service    :8081   (auth, JWT, profiles)
  ├── Menu Service    :8082   (restaurants, menu items)
  └── Order Service   :8083   (orders — requires Bearer token)
```

Inter-service calls (Order → User to validate tokens, Order → Menu to fetch item prices) happen directly between services, bypassing the gateway.

---

## Running with Docker Compose

### 1. Create a `.env` file in the project root

```env
USER_DB_PASSWORD=user_pass
MENU_DB_PASSWORD=menu_pass
ORDER_DB_PASSWORD=order_pass
JWT_SECRET=food-ordering-super-secret-jwt-key-32chars
```

The `.env` file is gitignored. The `JWT_SECRET` must be **at least 32 characters** or the User Service will refuse to start.

### 2. First run (or after schema changes)

```bash
# Drop old volumes and rebuild all images from scratch
docker compose down -v
docker compose up --build
```

`-v` drops the database volumes. This is required whenever the database schema changes (e.g. a column type changed) because Hibernate's `ddl-auto=update` cannot migrate types automatically.

### 3. Subsequent runs (no schema changes)

```bash
docker compose up --build
```

Or without rebuilding if code hasn't changed:

```bash
docker compose up
```

### 4. Stop everything

```bash
docker compose down
```

Add `-v` to also wipe the databases.

### 5. Start only a single database (for local development)

When running a service locally from your IDE, start just its database:

```bash
docker compose up user-db
docker compose up menu-db
docker compose up order-db
```

### Ports exposed to your host

| Container | Host port | What it is |
|---|---|---|
| api-gateway | 8080 | Single entry point for all API calls |
| user-microservice | 8081 | Direct access (or via gateway at `/users/**`) |
| menu-microservice | 8082 | Direct access (or via gateway at `/menu/**`) |
| order-microservice | 8083 | Direct access (or via gateway at `/orders/**`) |
| user-db | 5435 | PostgreSQL for user data |
| menu-db | 5433 | PostgreSQL for menu data |
| order-db | 5434 | PostgreSQL for order data |

---

## Running with Kubernetes

The `k8s/` directory contains manifests for deploying to any Kubernetes cluster (tested with local clusters like minikube or Docker Desktop Kubernetes).

### Prerequisites

- `kubectl` configured and pointing at your cluster
- Docker images built and available to the cluster (see below)

### 1. Build images and make them available

```bash
docker build -t user-microservice:latest ./user-microservice
docker build -t menu-microservice:latest ./menu-microservice
docker build -t order-microservice:latest ./order-microservice
docker build -t api-gateway:latest ./api-gateway
```

For **minikube**, load images into its registry:

```bash
minikube image load user-microservice:latest
minikube image load menu-microservice:latest
minikube image load order-microservice:latest
minikube image load api-gateway:latest
```

For **Docker Desktop Kubernetes**, the images are already available as long as they are built locally.

### 2. (Optional) Update secrets

`k8s/secrets.yaml` contains base64-encoded default values. To use your own:

```bash
echo -n "your-secret-value" | base64
```

Replace the corresponding value in `k8s/secrets.yaml` before applying.

### 3. Apply all manifests

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/user-db.yaml
kubectl apply -f k8s/menu-db.yaml
kubectl apply -f k8s/order-db.yaml
kubectl apply -f k8s/user-service.yaml
kubectl apply -f k8s/menu-service.yaml
kubectl apply -f k8s/order-service.yaml
kubectl apply -f k8s/api-gateway.yaml
```

Or apply everything at once:

```bash
kubectl apply -f k8s/
```

All resources are created in the `food-ordering` namespace.

### 4. Check that pods are running

```bash
kubectl get pods -n food-ordering
```

Wait until all pods show `Running` and `READY 1/1`.

### 5. Access the gateway

The gateway is exposed as a `NodePort` service on port **30080**.

- **Docker Desktop / localhost**: `http://localhost:30080`
- **minikube**: `http://$(minikube ip):30080`

### 6. Tear down

```bash
kubectl delete -f k8s/
```

---

## Swagger UI

Each service (except the gateway) has its own interactive API documentation. Use "Try it out" to execute requests directly from the browser.

| Service | Swagger URL |
|---|---|
| User Service | http://localhost:8081/swagger-ui/index.html |
| Menu Service | http://localhost:8082/swagger-ui.html |
| Order Service | http://localhost:8083/swagger-ui.html |

### How to authenticate in Swagger

Most endpoints require a JWT token. Here is the flow:

1. Open the **User Service** Swagger at `http://localhost:8081/swagger-ui/index.html`
2. Find `POST /api/users/login` and click **Try it out**
3. Enter your credentials and execute:
   ```json
   {
     "username": "admin",
     "password": "admin123"
   }
   ```
4. Copy the `token` value from the response body
5. Click the **Authorize** button (lock icon, top right of the page)
6. In the `bearerAuth` field paste the token — do **not** include the `Bearer ` prefix, Swagger adds it automatically
7. Click **Authorize** then **Close**

All subsequent "Try it out" calls from that Swagger page will now include the token automatically.

To use the token on the **Order Service** Swagger (`http://localhost:8083/swagger-ui.html`), repeat steps 5–7 on that page with the same token.

---

## API Endpoints

All endpoints are accessible via the API Gateway (`http://localhost:8080`) using the path prefix shown, or directly on each service's own port.

---

### User Service — `http://localhost:8081`
Gateway prefix: `/users/**` → rewrites to `/api/users/**`

#### Auth (public)

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/users/register` | Register a new customer account |
| `POST` | `/api/users/login` | Login and receive a JWT token |

**Register body:**
```json
{
  "username": "john",
  "email": "john@example.com",
  "password": "secret123"
}
```

**Login body:**
```json
{
  "username": "john",
  "password": "secret123"
}
```

**Login response:**
```json
{
  "token": "<jwt>",
  "username": "john",
  "role": "CUSTOMER"
}
```

#### User profile (requires Bearer token)

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/users/me` | Get your own profile |
| `PUT` | `/api/users/me` | Update your own profile |
| `GET` | `/api/users` | List all users (ADMIN only) |

#### Admin — user management (ADMIN token required)
Gateway prefix: none — call admin endpoints directly on port 8081 or add `/users/` prefix via gateway

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/admin/users` | Create a user with any role |
| `GET` | `/api/admin/users` | List all users (filter by `?role=CUSTOMER\|ADMIN`) |
| `GET` | `/api/admin/users/{id}` | Get user by ID |
| `PUT` | `/api/admin/users/{id}` | Update username, email, password, or role |
| `DELETE` | `/api/admin/users/{id}` | Delete a user |

---

### Menu Service — `http://localhost:8082`
Gateway prefix: `/menu/**` → rewrites to `/api/menu/**`

All menu endpoints are public (no token required).

#### Restaurants

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/menu/restaurants` | List all restaurants (filter by `?name=...`) |
| `GET` | `/api/menu/restaurants/{id}` | Get a restaurant and its full menu |
| `POST` | `/api/menu/restaurants` | Create a restaurant |
| `PUT` | `/api/menu/restaurants/{id}` | Update a restaurant / toggle active |

**Create restaurant body:**
```json
{
  "name": "Pizza Palace",
  "address": "123 Main St",
  "active": true
}
```

#### Menu items

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/menu/restaurants/{id}/items` | Add a menu item to a restaurant |
| `PUT` | `/api/menu/items/{id}` | Update a menu item |
| `GET` | `/api/menu/items/{id}` | Get a single item (used internally by Order Service) |

**Add menu item body:**
```json
{
  "name": "Margherita",
  "description": "Classic tomato and mozzarella",
  "price": 9.99,
  "available": true
}
```

---

### Order Service — `http://localhost:8083`
Gateway prefix: `/orders/**` → rewrites to `/api/orders/**`

**All order endpoints require a Bearer token.** The user ID is read from the token — you do not need to supply it in the request body.

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/orders` | Place a new order |
| `GET` | `/api/orders/{id}` | Get an order by ID |
| `GET` | `/api/orders/user/{userId}` | Get all orders for a user |
| `PUT` | `/api/orders/{id}/confirm` | Confirm a `CREATED` order |
| `PUT` | `/api/orders/{id}/complete` | Complete a `CONFIRMED` order |
| `PUT` | `/api/orders/{id}/cancel` | Cancel an order (not if already `COMPLETED`) |

**Order statuses:** `CREATED` → `CONFIRMED` → `COMPLETED` (or `CANCELLED` from any state except `COMPLETED`)

**Place order body:**
```json
{
  "items": [
    { "menuItemId": "<uuid>", "quantity": 2 },
    { "menuItemId": "<uuid>", "quantity": 1 }
  ]
}
```

The service calls the Menu Service to look up current prices and the User Service to confirm the user exists before saving.

---

## Typical end-to-end flow

```
1. POST /api/users/register        — create account
2. POST /api/users/login           — get JWT token
3. GET  /api/menu/restaurants      — browse restaurants
4. GET  /api/menu/restaurants/{id} — view menu
5. POST /api/orders                — place order (Bearer token required)
6. PUT  /api/orders/{id}/confirm   — confirm order
7. PUT  /api/orders/{id}/complete  — mark complete
```

---

## Default admin account

A default admin account is created on first startup:

| Field | Default value |
|---|---|
| Username | `admin` |
| Password | `admin123` |
| Email | `admin@foodordering.local` |

Change the password immediately in any non-local environment. The values can be overridden with `APP_ADMIN_USERNAME`, `APP_ADMIN_EMAIL`, and `APP_ADMIN_PASSWORD` environment variables (or the corresponding Kubernetes secret keys).
