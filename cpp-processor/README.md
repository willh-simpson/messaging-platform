## C++ Message Processor
High-performance Kafka consumer with time-windowed message batching and a gRPC stats server.

Written in C++20 using librdkafka and gRPC.

---

### What this does
Consumes messages from `messages.inbound` Kafka topic and accumulates them into per-channel batches.
A batch flushes when it either reaches 50 messages or has been open for 100ms.
Time trigger of 100ms is necessary to prevent low-volume channels waiting indefinitely to reach the max batch size.

Batch throughput stats are exposed with gRPC and is queried by **API Gateway** at:
* `GET /api/processor/stats/system`
* `GET /api/processor/stats/channels`
* `GET /api/processor/stats/channels/{channel_id}`

---

### Why C++
See `docs/architecture-decisions.md: ADR 4` for a breakdown on why this service was designed and implemented with C++ and not Java / Spring Boot.

---

### Architecture
```
Kafka (messages.inbound)
       |
       | [KafkaConsumer - dedicated thread]
       v
   MessageBatcher
   (per-channel ChannelBatch, mutex-guarded)
       |
       | [size >= 50 OR age >= 100ms]
       | [flush thread, 50ms poll interval]
       v
   onBatchFlushed callback -> logs throughput stats
   
   gRPC server [port 9090]
   ├> GetChannelStats(channelId) -> BatchStats
   └> GetSystemStats()           -> SystemStats
```

---

### Building
#### Prerequisites
* CMake 3.20+
* C++20-supported compiler (MSVC 2019+, GCC 11+, Clang 13+)
* vcpkg with the packages:
  * `protobuf`
  * `grpc`
  * `librdkafka`

#### Windows (MSVC)
`CMakePresets.json` handles compiler and toolchain config. Fill in your MSVC and Windows SDK paths in `CMakePresets.json` before building.
**See the `windows-debug` preset for required fields.**

```
cmake --preset windows-debug
cmake --build build
```
**Binary**: `build/cpp-processor.exe`

#### Linux / macOS
```
sudo apt-get install -y cmake build-essential libprotobuf-dev protobuf-compiler \
    libgrpc++-dev protobuf-compiler-grpc librdkakfa-dev         # linux

brew install cmake protobuf grpc librdkafka                     # macOS

cmake -B build -DCMAKE_BUILD_TYPE=Release
cmake --build build --parallel
```
**Binary**: `build/cpp-processor`

---

### Running
Configuration is read from environment variables, consistent with Java services:

| Variable         | Default            | Description              |
|------------------|--------------------|--------------------------|
| `KAFKA_BROKERS`  | `localhost:9092`   | Kafka bootstrap servers  |
| `KAFKA_TOPIC`    | `messages.inbound` | Consumed topic           |
| `KAFKA_GROUP_ID` | `cpp-processors`   | Consumer group ID        |
| `GRPC_HOST`      | `0.0.0.0`          | gRPC server bind address |
| `GRPC_PORT`      | `9090`             | gRPC server port         |

```
# With docker compose infrastructure running
./build/cpp-processor
```

#### Expected output:
```
[Main] cpp-processor starting
[Main] Kafka brokers: localhost:9092
[KafkaConsumer] Subscribed to messages.inbound as group cpp-processors
[Main] gRPC server listening on 0.0.0.0:9090
[Main] All components started. Ctrl+C to stop.
```

---

### Querying gRPC server
Server reflection is enabled, so no `.proto` file is needed with grpcurl.

#### via grpcurl
```
# List available services
grpcurl -plaintext localhost:9090 list

# System-wide stats
grpcurl -plaintext -d '{}' localhost:9090 \
    messaging.processor.MessageProcessorService/GetSystemStats
    
# Per-channel stats
grpcurl -plaintext \
    -d '{"channel_id": "<uuid>"}' localhost:9090 \
    messaging.processor.MessageProcessorService/GetChannelStats
```

#### via API Gateway
``` 
GET /api/processor/stats/system
GET /api/processor/stats/channels/{channel_id}
```