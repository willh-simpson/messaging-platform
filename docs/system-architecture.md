## System Architecture

### Full System Diagram
```mermaid
graph TB
    subgraph Clients
        A[Android App]
        W[Web App]
        D[Desktop App]
    end
    
    subgraph API Layer
        GW[API Gateway<br/>:8080<br/>JWT · Auth · Channels · Users]
    end
    
    subgraph Core Services
        MS[Messaging Service<br/>:8081<br/>Publish pipeline]
        PS[Persistence Service<br/>:8082<br/>Message history]
        DS[Delivery Service<br/>:8083<br/>WebSocket fan-out]
        CPP[C++ Processor<br/>:9090<br/>Batching · gRPC stats]
    end
    
    subgraph Data Stores
        PG[(PostgreSQL<br/>Users · Channels · Members)]
        MG[(MongoDB<br/>Message history<br/>Failed messages)]
        RD[(Redis<br/>User-Channel membership cache)]
    end
    
    subgraph Streaming
        KF[Kafka<br/>messages.inbound<br/>6 partitions]
    end
    
    subgraph Real-Time
        RMQ[RabbitMQ<br/>STOMP relay]
    end
    
    subgraph Observability / Monitoring
        PROM[Prometheus]
        GRAF[Grafana]
    end
    
    A & W & D -->|HTTPS| GW
    A & W & D -->|WebSocket / STOMP| DS
    
    GW -->|publish| MS
    GW -->|history query| PS
    GW -->|gRPC GetStats| CPP
    GW --- PG
    
    MS -->|membership check| RD
    MS -->|membership fallback| PG
    MS -->|MessageCreatedEvent| KF
    
    KF -->|consumer group: persistence| PS
    KF -->|consumer group: delivery| DS
    KF -->|consumer group: cpp| CPP
    
    PS --- MG
    
    DS -->|STOMP relay| RMQ
    RMQ -->|fan-out to all pods| DS
    
    GW & MS & PS & DS -->|metrics| PROM
    PROM --> GRAF
```

---

### Message Publish Flow (End-to-End)
```mermaid
sequenceDiagram
    participant C as Client
    participant GW as API Gateway
    participant MS as Messaging Service
    participant RD as Redis
    participant PG as PostgreSQL
    participant KF as Kafka
    participant PS as Persistence Service
    participant MG as MongoDB
    participant DS as Delivery Service
    participant WS as WebSocket Clients
    
    C->>GW: POST /api/messages {channel_id, content}
    GW->>GW: Validate JWT, extract userId
    GW->>MS: Forward request + X-Correlation-Id
    
    MS->>RD: isMember(channelId, userId)?
    alt Cache Hit
        RD-->>: true (~0.1ms)
    else Cache Miss
        MS->>PG: existsByChannelIdAndUserId(channelId, userId)
        PG-->>MS: true (~5ms)
        MS->>RD: cacheMembership(channelId, userId, TTL=5min)
    end
    
    MS->>KF: publish MessageCreatedEvent<br/>(partitionKey=channelId)
    MS-->>C: 202 Accepted {message_id}
    
    par Persistence
        KF->>PS: consume MessageCreatedEvent
        PS->>MG: save Message (id=messageId, idempotent)
    and Delivery
        KF->>DS: consume MessageCreatedEvent
        DS->>WS: STOMP /topic/channels/{channel_id}
    end
```

---

### Kafka Topology
```mermaid
graph LR
    subgraph Topic: messages.inbound - 6 partitions
        P0[Partition 0<br/>Channel A messages]
        P1[Partition 1<br/>Channel B messages]
        P2[Partition 2<br/>Channel C messages]
        P3[Partition 3]
        P4[Partition 4]
        P5[Partition 5]
    end
    
    subgraph persistence-consumers group
        PC1[Persistence Pod 1]
        PC2[Persistence Pod 2]
        PC3[Persistence Pod 3]
    end
    
    subgraph delivery-consumers group
        DC1[Delivery Pod 1]
        DC2[Delivery Pod 2]
    end
    
    subgraph cpp-processors group
        CPP[C++ Processor]
    end
    
    P0 & P1 --> PC1
    P2 & P3 --> PC2
    P4 & P5 --> PC3
    
    P0 & P1 & P2 --> DC1
    P3 & P4 & P5 --> DC2
    
    P0 & P1 & P2 & P3 & P4 & P5 --> CPP
```

**Ordering guarantee**: All messages for any given channel land on the same partition (`partitionKey = channelId`). 
Kafka guarantees ordering within a partition, therefore consumers see messages in the exact sequence they were published.

---

### Kubernetes Deployment
```mermaid
graph TB
    subgraph Internet
        USER[Client]
    end
    
    subgraph Kubernetes Cluster - namespace: messaging
        ING[Nginx Ingress<br/>api.messaging.example.com]
        
        subgraph Services
            GWS[api-gateway-service]
            MSS[messaging-service-service]
            PSS[persistence-service-service]
            DSS[delivery-service-service]
        end
        
        subgraph Deployments
            GWD[API Gateway<br/>2 replicas]
            MSD[Messaging Service<br/>2-6 replicas HPA]
            PSD[Persistence Service<br/>2-6 replicas HPA]
            DSD[Delivery Service<br/>2 replicas]
        end
        
        subgraph Config
            CM[ConfigMap<br/>non-sensitive config]
            SEC[Secret<br/>JWT · DB · RabbitMQ passwords]
        end
    end
    
    USER --> ING
    ING -->|/api/auth, /api/users,<br/>/api/channels| GWS --> GWD
    ING -->|/api/messages| MSS --> MSD
    ING -->|/api/channels/*/messages| PSS --> PSD
    ING -->|/ws| DSS --> DSD
    
    CM & SEC -.->|envFrom| GWD & MSD & PSD & DSD
```

---

### Dead Letter Topic: Failure Handling
```mermaid
stateDiagram-v2
    [*] --> Consumed: Kafka delivers event
    
    Consumed --> Persisted: MongoDB save succeeds
    Persisted --> [*]
    
    Consumed --> Retry1: Exception thrown
    Retry1 --> Retry2: Still failing (backoff 1s)
    Retry2 --> Retry3: Still failing (backoff 2s)
    Retry3 --> DLT: Still failing (backoff 4s)
    
    DLT --> FailedMessages: DltHandlerService saves\nto failed_messages collection
    
    FailedMessages --> PENDING: status = PENDING
    PENDING --> ACKNOWLEDGED: Operator reviews via\nGET /api/admin/failed-messages
    ACKNOWLEDGED --> RESOLVED: Root cause fixed,\nmessage replayed manually
    
    note right of DLT
        persistence_dlt_messages_total
        counter incremented ->
        Grafana panel turns red
    end note
```