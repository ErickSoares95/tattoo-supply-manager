# 🚀 Tattoo Supply Manager

Backend system designed to manage tattoo supply products, built with a strong focus on **software architecture, scalability, enterprise resilience, and modern observability**.

---

## 🌐 Live Demo

- **Storefront:** https://tattoo-supply-manager-storefront.vercel.app — Next.js store (public catalog, cart, checkout, admin panel at `/admin`)
- **Backend API:** https://tattoo-supply-manager.onrender.com (Swagger UI at `/swagger-ui.html`)

Demo accounts (seeded via `docker/seed-demo-data.sql`):

| Role  | Email             | Password        |
|-------|-------------------|------------------|
| Admin | admin@demo.com    | Demo@Admin123    |
| Client| client@demo.com   | Demo@Client123   |

> The backend runs on Render's free tier, so the first request after a period of inactivity may take 30-50s to respond while the instance spins back up. The storefront runs on Vercel and stays warm.

The original React + Vite MVP is still up at [tattoo-supply-manager-frontend](https://github.com/ErickSoares95/tattoo-supply-manager-frontend) for reference, but the storefront above is the current, actively developed client.

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
- **User** → Authentication (JWT, login by email *or* CPF), account management, and Role-Based Access Control (`ADMIN`, `CLIENT`, `ATTENDANT`).
- **Product** → Inventory control, validation constraints, and product management.
- **Order** → Order creation, ownership-based authorization, and core business rules (stock validation, item pricing).
- **Payment** → Payment processing per order, publishing to Kafka (`payment.processed`) via a **transactional outbox** for downstream consumers — decoupled from `notification` via a real message broker, not just an in-process event.
- **Notification** → Async processing, per-channel retry isolation, failure persistence, and reprocessing — reacts to both in-process order events and Kafka payment events.
- **AI** → Retrieval-Augmented Generation assistant (local LLM via Ollama, no external API keys) — restock recommendations from real sales data and semantic Q&A grounded in the product catalog via `pgvector`.
- **Report** → Advanced SQL reporting (Postgres view + window functions) beyond typical JPA CRUD.
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

**✅ Transactional Outbox (Producer-side reliability)**
- A payment and its `PaymentProcessedEvent` commit together: the event is written to an `outbox_events` row inside the same DB transaction, never published to Kafka inline. A scheduled poller relays PENDING rows to the broker afterwards.
- Solves the dual-write problem — a broker outage (or a crash right after commit) can't lose the event or fail/hang the payment request. Rows that keep failing past a max-attempts threshold are parked as `FAILED` for inspection.
- The client-facing config is env-driven (`KAFKA_SECURITY_PROTOCOL`, `KAFKA_SASL_MECHANISM`, `KAFKA_SASL_JAAS_CONFIG`), so the same build runs against the local plaintext broker or a managed SASL_SSL one (e.g. Redpanda Serverless).

**✅ Kafka Consumer Resilience**
- Payment events published to Kafka are consumed idempotently: an `event_id` unique constraint is claimed via insert-before-process (not check-then-act), so a redelivered message can never be processed twice, even under concurrency.
- A `DefaultErrorHandler` with fixed backoff retries a failing message 3 times before routing it to a Dead Letter Topic (`payment.processed.DLT`) instead of blocking the consumer indefinitely — validated manually end-to-end with a malformed message sent directly via `kafka-console-producer`.

## 🤖 AI / RAG Assistant

A real Retrieval-Augmented Generation pipeline, running entirely on local infrastructure (Ollama) — no OpenAI key, no external API cost:

- **Semantic search:** the product catalog is embedded (`nomic-embed-text`) into a `pgvector` store (Spring AI's `VectorStore` abstraction, backed by the same Postgres instance — no extra infra), reindexed reactively on every product create/update via the existing domain events.
- **Grounded Q&A:** `POST /assistant/ask` embeds the question, retrieves the closest matching products by vector similarity, and grounds a local LLM's (`llama3.2:1b`) answer in that retrieved context.
- **Structured retrieval, not just vectors:** `GET /assistant/restock-recommendations` feeds the LLM a real SQL aggregation of sales history + current stock (not a vector search) — the two retrieval strategies coexist on purpose, matching what a production RAG system actually looks like.
- **Reactive, not manual:** a dedicated `aiExecutor` thread pool listens to `OrderRegisteredEvent` to signal low stock and to `ProductRegisteredEvent`/`ProductUpdatedEvent` to keep the vector index in sync, isolated from the `notification` module's own async pipeline.

## 📈 Advanced SQL Reporting

`GET /reports/product-sales` is backed by a real Postgres `VIEW` (`product_sales_report`), not another JPA aggregate query — joining `order_items`, `payments` (only `APPROVED`) and `products`, with **window functions** (`RANK() OVER`, `SUM() OVER()`) computing revenue rank and market-share percentage per product directly in SQL. The entity mapping it to (`@Subselect` + `@Immutable`) is read-only by design; the view itself is created/refreshed on every app boot via an `ApplicationRunner`, since it depends on tables Hibernate only creates at startup.

## ☸️ Kubernetes

Deployment manifests (`k8s/`) for the app + Postgres, validated against a real local cluster (Docker Desktop's Kubernetes), including an `initContainer` that polls the database port before the app container starts — Kubernetes has no built-in equivalent to Docker Compose's `depends_on: condition: service_healthy`, so without it the app would crash-loop against a database that isn't accepting connections yet.

## 🔐 Security Highlights

- Stateless JWT (`Authorization: Bearer <token>`), no sessions, CSRF disabled (pure REST API).
- Three roles: `ADMIN`, `CLIENT`, `ATTENDANT`. Public self-registration always creates a `CLIENT` — role promotion only happens through an authenticated `ADMIN`-only endpoint (protects against privilege-escalation via mass assignment).
- Passwords hashed with BCrypt; JWT signing secret read from environment (`JWT_SECRET`), never committed to source control.
- Method-level authorization (`@EnableMethodSecurity` + `@PreAuthorize`) declared on controllers, combined with row-level ownership checks in the service layer where role alone isn't enough (e.g. viewing an order).

## 🛠️ Tech Stack

**Backend**
- Java 21 (LTS)
- Spring Boot 3.3
- Spring Security (JWT + RBAC)
- Spring Data JPA / Hibernate
- Spring Retry & Spring Async
- Spring Kafka
- Spring AI (Ollama chat client + `pgvector` VectorStore)
- PostgreSQL (+ `pgvector` extension)
- Apache Kafka (KRaft, no Zookeeper)
- Ollama (local LLM runtime — `llama3.2:1b`, `nomic-embed-text`)
- Prometheus & Grafana (Observability)
- Springdoc OpenAPI (Swagger UI)
- Docker & Docker Compose · Kubernetes manifests

**Storefront**
- Next.js 16 (App Router) · React 19 · TypeScript
- Tailwind CSS v4 (design tokens as CSS custom properties)
- Deployed on Vercel

---

## ▶️ Running the Project

### Requirements
- Docker & Docker Compose
- Java 21+ (only needed for running outside Docker)

### Full stack (app + database + Kafka + Ollama + observability)

```bash
docker compose -f docker/docker-compose.yml up -d --build
```

| Service       | URL                                              |
|---------------|---------------------------------------------------|
| API           | http://localhost:8080                              |
| Swagger UI    | http://localhost:8080/swagger-ui.html              |
| Prometheus    | http://localhost:9090                              |
| Grafana       | http://localhost:3000 (admin/admin)                |

Also brings up `postgres` (with `pgvector`), `kafka` (KRaft, single node) and `ollama` (pulls `llama3.2:1b` + `nomic-embed-text` on first boot) — everything the AI and payment modules need, no external accounts required.

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
| GET    | `/products`, `/products/{id}` | Public               | Browse the catalog                     |
| POST/PUT/DELETE | `/products/**`       | `ADMIN`               | Manage the catalog                     |
| POST   | `/orders`                     | `CLIENT`, `ADMIN`     | Place an order                         |
| GET    | `/orders`                     | `CLIENT`, `ADMIN`     | List orders — `CLIENT` sees only their own, `ADMIN` sees all |
| GET    | `/orders/{id}`                | `CLIENT`, `ADMIN`     | View an order — `CLIENT` gets 403 on orders they don't own |
| POST   | `/orders/{orderId}/payments`  | `CLIENT`, `ADMIN`     | Pay for an order — publishes `PaymentProcessedEvent` to Kafka via the transactional outbox |
| GET    | `/orders/{orderId}/payments`  | `CLIENT`, `ADMIN`     | List an order's payment attempts (`CLIENT` gets 403 on orders they don't own) |
| POST   | `/notifications/reprocess`    | `ADMIN`               | Manually reprocess failed notifications |
| POST   | `/assistant/ask`              | `ADMIN`, `ATTENDANT`  | Semantic Q&A grounded in the product catalog (RAG) |
| GET    | `/assistant/restock-recommendations` | `ADMIN`, `ATTENDANT` | LLM restock suggestions from real sales data |
| GET    | `/reports/product-sales`      | `ADMIN`, `ATTENDANT`  | Revenue rank/share per product (SQL window functions) |

Full request/response contracts are available via Swagger UI once the app is running, or in the Postman collection at `docs/postman/`.

## 📊 Future Improvements

- Outbox Pattern for guaranteed event delivery.
- Circuit Breaker (Resilience4j) around external notification senders.
- Distributed tracing.
- Real payment gateway integration (Pix/card via Mercado Pago, Pagar.me or Stripe) — the `payment` module's approval rule is currently deterministic for demo purposes, not wired to a real processor.

## 📁 Project Structure (Simplified)

```
user/
product/
order/
payment/
notification/
ai/
report/
shared/
```

Each business module follows the same internal layering (`domain`, `application`, `infrastructure`, `presentation`) and communicates with the others only through `shared` or domain events — never by calling another module's service directly.

## 🎯 Purpose

This project was built to demonstrate:

- Strong backend fundamentals (JPA/Hibernate, transactions, Bean Validation, advanced SQL).
- Real-world architectural decisions (modular monolith, event-driven internal communication — both in-process and over a real message broker).
- Production-grade security practices (JWT, RBAC, ownership-based authorization).
- Resilience and failure handling under real async/retry/DLQ conditions.
- A test suite that actually runs end-to-end, not just unit tests in isolation.
- Applied AI (RAG) grounded in the system's own data, not a chatbot bolted on the side.
- A full client (Next.js storefront + admin panel) consuming the API in production, not just Swagger calls.

## 📎 Repository

https://github.com/ErickSoares95/tattoo-supply-manager

## 🤝 Author

Erick Soares
Backend Developer focused on Java and Software Architecture
