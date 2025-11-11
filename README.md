# 📝 GigTasker Task Service

This service is the **"Operations Manager"** of the GigTasker platform. It is the microservice responsible for all business logic and data management related to Tasks (or "Gigs").

It serves as the central "source of truth" for what tasks are available, what their statuses are, and who posted them. It also acts as a "publisher" by announcing when a new task is created.

---

## ✨ Core Responsibilities

* **Task CRUD:** Handles all Create, Read, Update, and Delete operations for tasks.
* **Security:** Acts as a secure **OAuth2 Resource Server**. It validates a Keycloak JWT on every request to ensure the user is authenticated.
* **Event Publishing:** When a new task is successfully created, it publishes a `task.created` event to the `task-exchange` in RabbitMQ. This decouples it from the `notification-service`.
* **Business Logic:** Contains critical business logic, such as the `assignTask()` method, which transitions a task's status from `OPEN` to `ASSIGNED`.
* **Data Provider:** Serves task data to the rest of the ecosystem, including batch-loading for other services (e.g., `POST /api/v1/tasks/batch`).

---

## 🛠️ Tech Stack

* **Framework:** Spring Boot 3
* **Language:** Java 25
* **Database:** Spring Data JPA with PostgreSQL
* **Messaging:** Spring AMQP (RabbitMQ) for publishing events.
* **Security:** Spring Security (OAuth2 Resource Server) for JWT validation.
* **Platform:**
    * Spring Cloud Config Client (for configuration)
    * Spring Cloud Netflix Eureka Client (for service discovery)

---

## 🔌 Service Communication

This service is a central hub, acting as both a "callee" (it gets called) and a "publisher" (it sends events).

### Inbound (Its API - `/api/v1/tasks`)

This service exposes the following internal endpoints, which are routed by the `api-gateway`:

* **`POST /`**: Creates a new task.
* **`GET /`**: Retrieves a list of all tasks.
* **`GET /{taskId}`**: Retrieves the details for a single task.
* **`GET /user/{userId}`**: Retrieves all tasks posted by a specific user.
* **`PUT /{taskId}/assign`**: (Internal) A secure endpoint called by the `bid-service` to change a task's status to `ASSIGNED`.
* **`POST /batch`**: (Internal) A secure endpoint called by the `bid-service`. Takes a `List<Long>` of task IDs and returns a `List<TaskDTO>` of their details.

### Outbound (Events Published to RabbitMQ)

* **Exchange:** `task-exchange`
* **Routing Key:** `task.created`
* **Payload:** `TaskDTO` (as JSON)
* **Purpose:** Notifies the ecosystem (specifically the `notification-service`) that a new task is available.

### Outbound (Calls to Other Services)

* **None.** This service is self-contained. It does not call any other services to do its job.

---

## ⚙️ Configuration

This service gets its configuration from the `config-server` on startup.

* **Base Config (`task-service.yml`):** Contains all database credentials, RabbitMQ connection info, and security settings (like the Keycloak `issuer-uri`).
* **Local Profile (`task-service-local.yml`):** Overrides the network settings for local development, specifically setting `eureka.instance.hostname: localhost` to fix the Docker networking issue.
* **Data Model:** This service is the owner of the `TaskStatus` enum (`OPEN`, `ASSIGNED`, etc.), which is stored in the database as a `String` using `@Enumerated(EnumType.STRING)`.

---

## 🚀 How to Run

1.  **Start Dependencies (CRITICAL):**
    * Run `docker-compose up -d` (for Postgres, RabbitMQ, Keycloak).
    * Start the `config-server`.
    * Start the `service-registry`.

2.  **Run this Service:**
    Once the config and registry are running, you can start this service.
    ```bash
    # From your IDE, run TaskServiceApplication.java
    # Or, from the command line:
    java -jar target/task-service-0.0.1.jar
    ```

This service will start on a **random port** (as defined by `server.port: 0`) and register itself with the Eureka `service-registry`.