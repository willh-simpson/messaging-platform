#include <iostream>
#include <iomanip>
#include <csignal>
#include <atomic>
#include <thread>
#include <chrono>
#include <string>
#include <cstdlib>

#include <grpcpp/grpcpp.h>
// Required for InitProtoReflectionServerBuilderPlugin()
#include <grpcpp/ext/proto_server_reflection_plugin.h>

#include "kafka/KafkaConsumer.hpp"
#include "batch/MessageBatcher.hpp"
#include "grpc/ProcessorServiceImpl.hpp"

static std::atomic<bool> g_shutdown{false};

void signalHandler(int signal) {
    std::cout << "\n[Main] Received signal " << signal
              << " — initiating graceful shutdown" << std::endl;
    g_shutdown.store(true);
}

struct Config {
    std::string kafkaBrokers;
    std::string kafkaTopic;
    std::string kafkaGroupId;
    std::string grpcHost;
    int         grpcPort;

    static Config fromEnv() {
        Config c;
        c.kafkaBrokers = getEnvOrDefault("KAFKA_BROKERS",  "localhost:9092");
        c.kafkaTopic   = getEnvOrDefault("KAFKA_TOPIC",    "messages.inbound");
        c.kafkaGroupId = getEnvOrDefault("KAFKA_GROUP_ID", "cpp-processors");
        c.grpcHost     = getEnvOrDefault("GRPC_HOST",      "0.0.0.0");
        c.grpcPort     = std::stoi(getEnvOrDefault("GRPC_PORT", "9090"));
        return c;
    }

private:
    static std::string getEnvOrDefault(
        const char* key,
        const char* defaultValue
    ) {
        const char* val = std::getenv(key);
        return val ? std::string(val) : std::string(defaultValue);
    }
};

int main() {
    std::signal(SIGINT,  signalHandler);
    std::signal(SIGTERM, signalHandler);

    Config config = Config::fromEnv();

    std::cout << "[Main] cpp-processor starting" << std::endl;
    std::cout << "[Main] Kafka brokers: " << config.kafkaBrokers << std::endl;
    std::cout << "[Main] Kafka topic:   " << config.kafkaTopic   << std::endl;
    std::cout << "[Main] gRPC address:  " << config.grpcHost
              << ":" << config.grpcPort << std::endl;

    messaging::MessageBatcher batcher;
    messaging::ProcessorServiceImpl grpcService(batcher);
    messaging::KafkaConsumer kafkaConsumer(
        config.kafkaBrokers,
        config.kafkaGroupId,
        config.kafkaTopic
    );

    auto onBatchFlushed = [](const messaging::ChannelBatch& batch) {
        double processingMs = std::chrono::duration<double, std::milli>(
            std::chrono::steady_clock::now() - batch.createdAt
        ).count();

        std::cout << "[Processor] Batch flushed:"
                  << " channel=" << batch.channelId
                  << " size="    << batch.messages.size()
                  << " latency=" << std::fixed << std::setprecision(2)
                  << processingMs << "ms" << std::endl;
    };

    kafkaConsumer.start([&batcher, &onBatchFlushed](
        const messaging::MessageEvent& event
    ) {
        batcher.add(event, onBatchFlushed);
    });

    std::thread flushThread([&batcher, &onBatchFlushed]() {
        std::cout << "[Main] Batch flush thread started (50ms interval)" << std::endl;
        while (!g_shutdown.load()) {
            std::this_thread::sleep_for(std::chrono::milliseconds(50));
            batcher.flushExpired(onBatchFlushed);
        }

        std::cout << "[Main] Final flush on shutdown..." << std::endl;
        batcher.flushExpired(onBatchFlushed);
    });

    std::string serverAddress =
        config.grpcHost + ":" + std::to_string(config.grpcPort);

    grpc::ServerBuilder builder;
    builder.AddListeningPort(serverAddress, grpc::InsecureServerCredentials());
    builder.RegisterService(&grpcService);

    grpc::reflection::InitProtoReflectionServerBuilderPlugin();

    std::unique_ptr<grpc::Server> server = builder.BuildAndStart();
    if (!server) {
        std::cerr << "[Main] Failed to start gRPC server on " << serverAddress << std::endl;
        g_shutdown.store(true);
        kafkaConsumer.stop();
        flushThread.join();
        return 1;
    }

    std::cout << "[Main] gRPC server listening on " << serverAddress << std::endl;
    std::cout << "[Main] All components started. Ctrl+C to stop." << std::endl;

    while (!g_shutdown.load()) {
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
    }

    std::cout << "[Main] Shutting down..." << std::endl;
    kafkaConsumer.stop();
    flushThread.join();

    server->Shutdown(
        std::chrono::system_clock::now() + std::chrono::seconds(5)
    );

    std::cout << "[Main] Shutdown complete" << std::endl;
    return 0;
}