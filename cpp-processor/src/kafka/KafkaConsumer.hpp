#pragma once

#include <string>
#include <functional>
#include <atomic>
#include <thread>
#include <librdkafka/rdkafkacpp.h>
#include "model/MessageEvent.hpp"

namespace messaging {
    /**
     * Wraps librdkafka's API for the processor.
     */
    class KafkaConsumer {
    public:
        using MessageCallback = std::function<void(const MessageEvent&)>;

        explicit KafkaConsumer(
            const std::string& brokers,
            const std::string& groupId,
            const std::string& topic
        );

        ~KafkaConsumer();

        // owns a live Kafka consumer connection, so it is non-copyable and non-movable
        KafkaConsumer(const KafkaConsumer&) = delete;
        KafkaConsumer& operator=(const KafkaConsumer&) = delete;

        /**
        * Starts consuming on a background thread. Calls onMessage for each valid MessageCreatedEvent received.
        * Call stop() to terminate.
        */
        void start(MessageCallback onMessage);
        /**
        * Signals consumer to stop and wait for thread to join.
        */
        void stop();

        bool isRunning() const { return running_.load(); }
    
    private:
        void consumeLoop(MessageCallback onMessage);
        bool tryDeserialize(const std::string& payload, MessageEvent& out) const;

        std::string brokers_;
        std::string groupId_;
        std::string topic_;

        std::unique_ptr<RdKafka::KafkaConsumer> consumer_;
        std::atomic<bool> running_{false};
        std::thread consumerThread_;
    };
}