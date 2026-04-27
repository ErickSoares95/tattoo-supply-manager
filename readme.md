# 🚀 Tattoo Supply Manager

Backend system designed to manage tattoo supply products, built with a strong focus on **software architecture, scalability, and evolution towards distributed systems**.

---

## 📌 Overview

This project goes beyond a simple CRUD.

It was designed as a **Modular Monolith (Modulith)** with clear boundaries between domains, allowing a **gradual and safe evolution into microservices**.

The main goal is to simulate real-world backend challenges such as:
- Decoupled communication
- Asynchronous processing
- Failure handling
- System resilience

---

## 🧠 Architecture

### Current approach

- Modular Monolith (Modulith)
- Clean separation of concerns
- Event-driven communication between modules

### Modules

- **Product** → product management
- **Order** → order creation and business rules
- **Notification** → async processing and external communication simulation

---

## 🔄 Architecture Evolution


Modular Monolith → Event-Driven → Microservices


The system is intentionally designed to support:
- gradual module extraction
- minimal refactoring when scaling
- independent service evolution

---

## ⚙️ Technical Decisions

- Use-case oriented services (action-based design)
- DTO-based API layer (decoupled from domain)
- Centralized exception handling
- Pagination and sorting with Spring Data
- Event-driven internal communication

---

## 🔔 Event-Driven Flow


Order created
↓
Event published (OrderRegisteredEvent)
↓
NotificationListener (async)
↓
NotificationService
↓
Retry mechanism
↓
Failure persistence (if needed)
↓
Reprocessing flow


---

## 💥 Resilience Strategy

The system handles failures in a production-like way:

### ✅ Retry mechanism
- Automatic retries using Spring Retry
- Configurable attempts and backoff

### ✅ Failure persistence
- Failed notifications are stored in the database
- Prevents data loss

### ✅ Reprocessing
- Endpoint to reprocess failed events
- Ensures eventual consistency

---

## 🚀 Features

- Product creation and management
- Order processing with business validation
- Stock control
- Asynchronous notification system
- Retry + failure recovery strategy
- Reprocessing of failed operations

---

## 🛠️ Tech Stack

- Java 21
- Spring Boot 3
- Spring Data JPA
- PostgreSQL
- Docker
- Spring Async
- Spring Retry

---

## ▶️ Running the project

### Requirements

- Docker
- Java 21

### Start application

```bash
docker-compose up --build
📡 Main Endpoints
Create Order
POST /orders
Reprocess failed notifications
POST /notifications/reprocess
📊 Future Improvements
Outbox Pattern
Message broker integration (RabbitMQ / Kafka)
Circuit Breaker (Resilience4j)
Observability (logs, metrics, tracing)
Authentication & Authorization
📁 Project Structure (Simplified)
product/
order/
notification/
shared/

Each module is isolated and communicates through events.

🎯 Purpose

This project was built to demonstrate:

Strong backend fundamentals
Real-world architectural decisions
System evolution strategy
Resilience and failure handling
📎 Repository

https://github.com/ErickSoares95/tattoo-supply-manager

🤝 Author

Erick Soares
Backend Developer focused on Java and Software Architecture

💡 Final Note

This is not just a project — it's an exploration of how to build systems that scale, evolve, and survive failure.