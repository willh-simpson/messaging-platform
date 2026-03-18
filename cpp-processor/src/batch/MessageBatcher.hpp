#pragma once

#include <string>
#include <vector>
#include <unordered_map>
#include <mutex>
#include <chrono>
#include <functional>
#include <atomic>
#include "model/MessageEvent.hpp"

namespace messaging {
    /**
     * One time-windowed acccumulation of messages for a channel.
     * Batches are ready when either MAX_BATCH_SIZE is reached or more than BATCH_WINDOW_MS have elapsed.
     */
    struct ChannelBatch {
        std::string channelId;
        std::vector<MessageEvent> messages;
        std::chrono::steady_clock::time_point createdAt;

        explicit ChannelBatch(std::string id): channelId(std::move(id)), createdAt(std::chrono::steady_clock::now()) {}

        bool isReady(size_t maxSize, std::chrono::milliseconds window) const {
            if (messages.size() >= maxSize) return true;
            auto age = std::chrono::steady_clock::now() - createdAt;

            return age >= window;
        }
    };

    /**
     * Per-channel throughput metrics.
     * Queried by gRPC service to respond to GetChannelStats calls.
     */
    struct BatchStats {
        std::string channelId;
        int64_t totalMessages{0};
        int64_t totalBatchesFlushed{0};
        int32_t currentBatchSize{0};
        double messagesPerSecond{0.0};
        int64_t lastBatchTimestampMs{0};
    };

    /**
     * Accumulates Kafka events into time-windowed batches.
     */
    class MessageBatcher {
    public:
        static constexpr size_t MAX_BATCH_SIZE = 50;
        static constexpr auto BATCH_WINDOW = std::chrono::milliseconds(100);
        static constexpr double EWMA_ALPHA = 0.3;

        using BatchCallback = std::function<void(const ChannelBatch&)>;

        /**
         * Adds message to pending batch for its channel.
         * Triggers immediate flush if batch size limit is reached.
         */
        void add(const MessageEvent& event, const BatchCallback& onFlush);

        /**
         * Checks all pending batches for time-window expiry and flush those that are ready.
         */
        void flushExpired(const BatchCallback& onFlush);

        /**
         * Get snapshot of stats for a specific channel.
         * Returns nullopt if channel has never been seen.
         */
        std::optional<BatchStats> getChannelStats(const std::string& channelId) const;

        /**
         * Get aggregate stats across all channels.
         */
        struct SystemStats {
            int64_t totalMessages{0};
            int64_t totalBatches{0};
            int32_t activeChannels{0};
            double overallThroughput{0.0};
            int64_t uptimeSeconds{0};
        };
        SystemStats getSystemStats() const;

        int32_t workerThreadCount() const { return WORKER_THREADS; }
    
    private:
        static constexpr int32_t WORKER_THREADS = 4;

        void flushBatch(
            const std::string& channelId,
            ChannelBatch& batch,
            const BatchCallback& onFlush
        );

        mutable std::mutex batchMutex_;
        std::unordered_map<std::string, ChannelBatch> batches_;
        std::unordered_map<std::string, BatchStats> stats_;

        std::chrono::steady_clock::time_point startTime_{
            std::chrono::steady_clock::now()
        };
    };
}