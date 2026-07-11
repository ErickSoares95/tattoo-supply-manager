# 🚀 Tattoo Supply Manager

Backend system designed to manage tattoo supply products, built with a strong focus on **software architecture, scalability, enterprise resilience, and modern observability**.

---

## 🌐 Live Demo

- **Frontend:** https://tattoo-supply-manager-frontend.onrender.com
- **Backend API:** https://tattoo-supply-manager.onrender.com (Swagger UI at `/swagger-ui.html`)

Demo accounts (seeded via `docker/seed-demo-data.sql`):

| Role  | Email             | Password        |
|-------|-------------------|------------------|
| Admin | admin@demo.com    | Demo@Admin123    |
| Client| client@demo.com   | Demo@Client123   |

> Both services run on Render's free tier, so the first request after a period of inactivity may take 30-50s to respond while the instance spins back up.

---

## 📌 Overview

This project goes beyond a simple CRUD. It was designed as a **Modular Monolith (Modulith)** with clear boundaries between domains, allowing a **gradual and safe evolution into microservices**.

The main goal is to simulate real-world enterprise backend challenges such as decoupled communication, system resilience, comprehensive testing, RBAC security, and live telemetry.

---

## 🧠 Architecture

### Current Approach
- **Modular Monolith (Modulith):** Domain segregation ensuring clean boundaries.
- **Clean Separation of Concerns:** Layered architecture designed around specific business use cases.
- **Event-Driven Communication:** Loose coupling between internal modules via asynchronous events (`order` never calls `notification` directly).

### Modules
- **User** → Authentication (JWT), account management, and Role-Based Access Control (`ADMIN`, `CLIENT`, `ATTENDANT`).
- **Product** → Inventory control, validation constraints, and product management.
- **Order** → Order creation, ownership-based authorization, and core business rules (stock validation, item pricing).
- **Notification** → Async processing, per-channel retry isolation, failure persistence, and reprocessing.
- **Shared** → Cross-cutting concerns: JWT security, global exception handling, base entity auditing.

---

## 🔄 Architecture Evolution

Modular Monolith ──> Event-Driven ──> Microservices

The system is intentionally engineered to support gradual module extraction with minimal refactoring, enabling independent service evolution as organizational demands grow.

## ⚙️ Technical Decisions

**Use-Case Driven Services:** Action-based design patterns isolating business logic (one class per use case, not fat multi-method services).

**Strict API Layer Decoupling:** DTOs (`record`) embedded with robust Bean Validations (`@Valid`, `@NotEmpty`, `@Positive`, `@DecimalMin`) protecting domain integrity — controllers never accept or return JPA entities.

**Enterprise Security:** Stateless JWT authentication + Role-Based Access Control (RBAC) powered by Spring Security, protecting endpoints via `@PreAuthorize`. Signing secret and expiration are externalized via environment variables, never hardcoded.

**Ownership-Based Authorization:** `CLIENT` users only see and access their own orders; `ADMIN` users have full visibility. Enforced at the service layer via `AccessDeniedException`, reusing the same global error-handling path as method-level security.

**Global Exception Handling:** Centralized controller advice (`@RestControllerAdvice`) translating domain and validation errors into semantic, RFC-compliant HTTP responses — every domain exception extends a common `BaseException`, so new business rules never require new handler code.

**High-Quality Test Suite:** Unit tests (Mockito) for every service, plus full integration tests (`@SpringBootTest`, `MockMvc`, real PostgreSQL) validating security tokens, authenticated user context, and database state end-to-end.

## 📊 Observability & Telemetry

The application exposes real-time telemetry, enabling proactive production monitoring through a modern observability stack:

- **Spring Boot Actuator & Micrometer:** Native application metrics gathering.
- **Prometheus:** High-performance time-series database scraping application metrics.
- **Grafana:** Custom dashboards tracking JVM state (G1 GC, Heap/Non-Heap memory), CPU utilization, thread allocation, and active HikariCP database connection pools.

## 💥 Resilience Strategy

The system architecture treats infrastructure and external failures as first-class citizens:

**✅ Retry Mechanism**
- Automatic, per-channel retries powered by Spring Retry — a failure in one notification channel (e.g. webhook) never causes a duplicate resend on a channel that already succeeded (e.g. email).
- Configurable backoff policy preventing cascade system exhaustion.

**✅ Failure Persistence & Eventual Consistency**
- Failed asynchronous notifications are captured and stored in the database instead of being silently lost.
- An administrative endpoint (`ADMIN`-only) triggers reprocessing of pending failures; a notification is only marked as resolved when redelivery genuinely succeeds.
- Domain events are only published to listeners **after the originating transaction commits** (`@TransactionalEventListener(phase = AFTER_COMMIT)`), avoiding notifications for orders that end up rolled back.

## 🔐 Security Highlights

- Stateless JWT (`Authorization: Bearer <token>`), no sessions, CSRF disabled (pure REST API).
- Three roles: `ADMIN`, `CLIENT`, `ATTENDANT`. Public self-registration always creates a `CLIENT` — role promotion only happens through an authenticated `ADMIN`-only endpoint (protects against privilege-escalation via mass assignment).
- Passwords hashed with BCrypt; JWT signing secret read from environment (`JWT_SECRET`), never committed to source control.
- Method-level authorization (`@EnableMethodSecurity` + `@PreAuthorize`) declared on controllers, combined with row-level ownership checks in the service layer where role alone isn't enough (e.g. viewing an order).

## 🛠️ Tech Stack

- Java 21 (LTS)
- Spring Boot 3.3
- Spring Security (JWT + RBAC)
- Spring Data JPA / Hibernate
- Spring Retry & Spring Async
- PostgreSQL
- Prometheus & Grafana (Observability)
- Springdoc OpenAPI (Swagger UI)
- Docker & Docker Compose

---

## ▶️ Running the Project

### Requirements
- Docker & Docker Compose
- Java 21+ (only needed for running outside Docker)

### Full stack (app + database + observability)

```bash
docker compose -f docker/docker-compose.yml up -d --build
```

| Service       | URL                                              |
|---------------|---------------------------------------------------|
| API           | http://localhost:8080                              |
| Swagger UI    | http://localhost:8080/swagger-ui.html              |
| Prometheus    | http://localhost:9090                              |
| Grafana       | http://localhost:3000 (admin/admin)                |

### Running locally against Docker's database only

```bash
docker compose -f docker/docker-compose.yml up -d postgres
./mvnw spring-boot:run
```

### Running the tests

```bash
./mvnw test
```

Runs the full suite (unit + integration) against a real PostgreSQL instance — make sure `postgres` is up first, as described above.

---

## 📡 Main Endpoints

| Method | Endpoint                     | Access              | Description                          |
|--------|-------------------------------|----------------------|---------------------------------------|
| POST   | `/users`                      | Public                | Register a new account (always `CLIENT`) |
| POST   | `/auth/login`                 | Public                | Authenticate and receive a JWT         |
| GET    | `/users`, `/users/{id}`       | `ADMIN`               | List / view accounts                   |
| PUT    | `/users/{id}`                 | `ADMIN`               | Update an account, including role promotion |
| GET    | `/products`, `/products/{id}` | Any authenticated user | Browse the catalog                     |
| POST/PUT/DELETE | `/products/**`       | `ADMIN`               | Manage the catalog                     |
| POST   | `/orders`                     | `CLIENT`, `ADMIN`     | Place an order                         |
| GET    | `/orders`                     | `CLIENT`, `ADMIN`     | List orders — `CLIENT` sees only their own, `ADMIN` sees all |
| GET    | `/orders/{id}`                | `CLIENT`, `ADMIN`     | View an order — `CLIENT` gets 403 on orders they don't own |
| POST   | `/notifications/reprocess`    | `ADMIN`               | Manually reprocess failed notifications |

Full request/response contracts are available via Swagger UI once the app is running.

## 📊 Future Improvements

- Outbox Pattern for guaranteed event delivery.
- Message broker integration (RabbitMQ / Kafka) replacing the in-process event bus.
- Circuit Breaker (Resilience4j) around external notification senders.
- Distributed tracing.
- React frontend consuming this API.

## 📁 Project Structure (Simplified)

```
user/
product/
order/
notification/
shared/
```

Each business module follows the same internal layering (`domain`, `application`, `infrastructure`, `presentation`) and communicates with the others only through `shared` or domain events — never by calling another module's service directly.

## 🎯 Purpose

This project was built to demonstrate:

- Strong backend fundamentals (JPA/Hibernate, transactions, Bean Validation).
- Real-world architectural decisions (modular monolith, event-driven internal communication).
- Production-grade security practices (JWT, RBAC, ownership-based authorization).
- Resilience and failure handling under real async/retry conditions.
- A test suite that actually runs end-to-end, not just unit tests in isolation.

## 📎 Repository

https://github.com/ErickSoares95/tattoo-supply-manager

## 🤝 Author

Erick Soares
Backend Developer focused on Java and Software Architecture
