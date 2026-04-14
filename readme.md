# 🧾 Tattoo Supply Manager

Backend system for managing tattoo products, designed with a strong focus on **scalability**, **modular architecture**, and **evolution towards microservices**.

---

## 🚀 Tech Stack

- Java 21
- Spring Boot 3
- Spring Data JPA
- PostgreSQL
- Docker & Docker Compose
- Maven

---

## 🧠 Architecture Overview

This project follows a **Modular Monolith (Modularith)** approach, structured around domain boundaries and designed for **gradual evolution into microservices**.

### 🔹 Architectural Principles

- Use-case oriented services (application layer)
- Clear separation of concerns (Controller → Service → Repository)
- DTO-based communication (API decoupling)
- Centralized exception handling
- Pagination and sorting via Spring Data
- Event-driven communication between modules
- Inspired by Clean Architecture & DDD

---

## 🧩 Module Design

The system is divided into independent modules with low coupling.

Example: product → core business logic
 		notification → side effects (event-driven)
		 shared → cross-cutting concerns

This structure enables safe and incremental extraction into microservices.

---

## 📦 Features

- Product creation  
- Paginated product listing  
- Find product by ID  
- Standardized error handling  
- Fully containerized environment  

---

## 🐳 Running with Docker

```bash
docker compose up --build
```



Application will be available at:

```
http://localhost:8080
```



## 💻 Running Locally

```
./mvnw clean install
./mvnw spring-boot:run
```

## 🔗 API Endpoints

| Method | Endpoint                               |
| ------ | :------------------------------------- |
| POST   | /products                              |
| GET    | /products?page=0&size=10&sort=name,asc |
| GET    | /products/{id}                         |

## 🎯 Roadmap

- Dynamic filtering (Specification)
- Bean Validation
- Authentication (JWT)
- Internal event-driven communication
- Observability (logs & metrics)
- Gradual migration to microservices

------

## 📈 Architectural Evolution

```
Modular Monolith → Event-Driven Modularith → Microservices
```

------

## 👨‍💻 Author

Erick de Lira Soares
Backend Software Engineer