## Distributed Real-Time Messaging Platform
A production-grade real-time messaging system.
Built from scratch to demonstrate distributed systems design, high-throughput event streaming, and cloud-native deployment.

> Architecture diagrams and system design documentation are in `docs/`.

---

### Architecture Overview
```
Clients -> API Gateway -> Kafka -> Persistence Service -> MongoDB (message history)
           |                       |
           v                       v
       Messaging           Delivery Service -> WebSocket -> Clients
       Service   <-> Redis (user cache)
           |
           v
     C++ Processor (gRPC stats)
```

Full documentation in `docs/system-architecture.md`.

---

### Tech Stack
| Layer                       | Technology                                               |
|-----------------------------|----------------------------------------------------------|
| API / Auth                  | Java 23, Spring Boot 3.3.5, Spring Security, JWT         |
| Event Streaming             | Apache Kafka                                             |
| Databases                   | PostgreSQL, MongoDB, Redis                               |
| Real-Time Delivery          | WebSocket / STOMP, RabbitMQ relay                        |
| High-Performance Processing | C++20, librdkafka, gRPC                                  |
| Observability / Monitoring  | Prometheus, Grafana, Micrometer, structured JSON logging |
| Infrastructure              | Docker, Kubernetes, Horizontal Pod Autoscaler            |

---

### Services
* **API Gateway** *(port 8080)*: Authentication (JWT), user and channel management, request routing, gRPC client to C++ processor.
* **Messaging Service** *(port 8081)*: Message publish pipeline. Validates channel membership against Redis (cache-aside, 5-minute TTL with PostgreSQL fallback), publishes `MessageCreatedEvent` to Kafka partitioned with per-channel ordering.
* **Persistence Service** *(port 8082)*: Kafka consumer that writes to MongoDB with idempotent inserts. `@RetryableTopic` with exponential backoff. Dead-letter failures are stored in queryable `failed_messages` collection with an operator assessment API.
* **Delivery Service** *(port 8083)*: Kafka consumer that broadcasts messages to WebSocket subscribers with STOMP. Uses RabbitMQ broker relay in production so multiple pods fan out correctly to all connected clients.
* **C++ Processor**: High-performance Kafka consumer. Time-windowed message-batching (50 messages or 100ms) with a gRPC stats server queried by **API Gateway**.

---

### Key Design Decisions
* **Fat event pattern**: `MessageCreatedEvent` carries all the data consumers need in order to eliminate cross-service queries on the hot path.
* **Idempotent Kafka consumers**: MongoDB uses `message_id` as `_id`. At-least-once delivery with duplicate key handling keeps retries safe to prevent duplicate entries.
* **Correlation ID tracing**: UUID generated at the gateway propagates through every Kafka event and HTTP header, linking logs across all services for a single request.
* **HPA bound by Kafka partition count**: **Persistence Service** scales to a max of 6 replicas, matching topic partition count, to avoid idle consumer pods.
