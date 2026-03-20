## Architecture Decision Records
Key technical decisions I made during this project, with reasoning and alternatives considered.

This is an archive of those decisions during the development process at different development phases/milestones.
These milestones are visible in repo branches `feat/milestone-n/`.

---

### ADR 1: Fat event pattern for Kafka messages
**Decision**: `MessageCreatedEvent` carries full message payload (content, author username/ID, channel ID) rather than just one ID and a pointer to fetch message data.

**Reasoning**: Delivery and persistence consumers need the full message payload as quickly as possible.
A thin event requires consumers to make synchronous HTTP or database calls back to the origin service to fetch the data,
which introduces latency, coupling, and failure modes along the hot path.

The full message payload is small (~500 bytes), making bandwidth cost negligible.

**Alternative considered**: Event sourcing with thin events.

Rejected this because it adds complexity (event store, replay infrastructure), which is not beneficial at this scale.

---

### ADR 2: MongoDB `_id` = `messageId` for idempotent writes
**Decision**: The MongoDB `Message` document uses `messageId` assigned by **Messaging Service** as the `_id` field rather than a generated `ObjectId`.

**Reasoning**: Kafka's at-least-once delivery may process the same event more than once after crash or re-balance.
Using `messageId` as `_id` catches any duplicate messages and ignores them,
enabling the database to enforce idempotency without extra application-level de-duplication logic.

**Alternative considered**: Generated `ObjectId` with application-level check (`findByMessageId()` before insert).

Rejected this because it introduces a race condition under concurrent consumption 2 round trips to the database per message.

---

### ADR 3: Redis cache-aside for user-channel membership checks
**Decision**: **Messaging Service** checks Redis before querying PostgreSQL database to verify channel membership with a 5-minute TTL.

**Reasoning**: Channel membership data is read on every message publish to ensure user is a channel member, but this data rarely changes. 
Before this cache every publish creates a synchronous database read. 

Using Redis the cache-hit rate approaches up to 99% at steady state, keeping PostgreSQL load extremely minimal on the publish path.

**Alternative considered**: In-memory cache within each service pod.

Rejected this because it cannot be validated across all pods, produces stale data after horizontal scaling, and loses all cached state on pod restart.

**Trade-off**: Any user who leaves a channel can publish messages for up to 5 minutes until TTL expires.

Planned stricter invalidation is a Kafka `membership.revoked` event consumed by **Messaging Service** to immediately revoke the key.

---

### ADR 4: C++ for batch processing
**Decision**: Message batch aggregation component is written in C++20 rather than Java.

**Reasoning**: JVM's garbage collector introduces pauses that cause latency spikes unpredictable enough to violate tight batching windows (target is 100ms).

C++ has deterministic memory management. This mirrors how Discord and other similar platforms use systems languages to support high-throughput pipelines while keeping business logic in other managed languages.

**Alternative considered**: Java with GC tuning (ZGC, low-pause settings).

Rejected this because GC tuning is fragile under variable load, and C++ component both demonstrates cross-language system design and results in simpler and more efficient implementation than a GC tuner.

---

### ADR 5: RabbitMQ STOMP relay for WebSocket delivery
**Decision**: **Delivery Service** uses RabbitMQ STOMP broker relay in production rather than Spring's in-memory broker (used in local dev).

**Reasoning**: Spring's in-memory broker only routes messages to WebSocket sessions connected to the same JVM instance. Using multiple delivery service pods would result in messages consumed by pod A never reaching clients connected to pod B, etc.

RabbitMQ acts as a shared message bus, so any pod can receive a Kafka event and RabbitMQ fans it out to pods holding the relevant client connections.

**Alternative considered**: Sticky sessions at the load balancer (routes the same client to the same pod).

Rejected this because it undermines horizontal scaling (one overloaded pod cannot shed load to others).
Clients reconnecting after a pod restart land on a different pod anyway.
