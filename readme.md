# 🧾 Tattoo Supply Manager

Backend system designed to simulate real-world tattoo studio operations, including product inventory and order management.

Built with a strong focus on **modular architecture, business rules, and scalability**, following a **Modular Monolith (Modulith)** approach, ready to evolve into microservices.

---

## 🚀 Tech Stack

- Java 21
- Spring Boot 3
- Spring Data JPA
- PostgreSQL
- Docker & Docker Compose
- Maven

---

## 🧠 Architecture

This project follows a **Modular Monolith architecture** structured by domain modules, combined with **clean separation of concerns**.

### 🔹 Key Concepts

- Use-case oriented services (application layer)
- Domain-driven modularization
- DTO-based API communication
- Centralized exception handling
- Pagination and filtering (Specification)
- Event-driven communication between modules

---

## 🧩 Modules

- **Product** → inventory management and stock validation
- **Order** → order processing and stock updates
- **Notification** → event-driven side effects (email, logs, integrations)

---

## 📦 Features

- Product creation and management
- Paginated listing with sorting and filtering
- Stock validation rules
- Event-driven communication between modules
- Centralized error handling
- Fully containerized environment

---

## 🐳 Running with Docker

```bash
docker compose up --build