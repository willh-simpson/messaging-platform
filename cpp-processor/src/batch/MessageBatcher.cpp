#include "MessageBatcher.hpp"

#include <iostream>
#include <algorithm>
#include <optional>
#include <chrono>

namespace messaging {
    void MessageBatcher::add(const MessageEvent& event, const BatchCallback& onFlush) {
        std::unique_lock lock(batchMutex_);

        const std::string& channelId = event.channelId;

        auto it = batches_.find(channelId);
        if (it == batches_.end()) {
            auto [inserted, _] = batches_.emplace(channelId, ChannelBatch(channelId));

            it = inserted;
        }

        it->second.messages.push_back(event);

        auto& stats = stats_[channelId];
        stats.channelId = channelId;
        stats.totalMessages++;
        stats.currentBatchSize = static_cast<int32_t>(it->second.messages.size());
        // each message arrival is treated as an instantaneous rate sample. computes into a running average
        stats.messagesPerSecond = EWMA_ALPHA * 1.0 + (1.0 - EWMA_ALPHA) * stats.messagesPerSecond;

        if (it->second.isReady(MAX_BATCH_SIZE, BATCH_WINDOW)) {
            flushBatch(channelId, it->second, onFlush);

            batches_.erase(it);
        }
    }

    void MessageBatcher::flushExpired(const BatchCallback& onFlush) {
        std::unique_lock lock(batchMutex_);

        // collect then flush to avoid iterator invalidation
        std::vector<std::string> toFlush;
        for (auto& [channelId, batch] : batches_) {
            if (batch.isReady(MAX_BATCH_SIZE, BATCH_WINDOW)) {
                toFlush.push_back(channelId);
            }
        }

        for (const auto& channelId : toFlush) {
            auto it = batches_.find(channelId);
            if (it != batches_.end()) {
                flushBatch(channelId, it->second, onFlush);

                batches_.erase(it);
            }
        }
    }

    void MessageBatcher::flushBatch(
        const std::string& channelId,
        ChannelBatch& batch,
        const BatchCallback& onFlush
    ) {
        if (batch.messages.empty()) return;

        auto& stats = stats_[channelId];
        stats.totalBatchesFlushed++;
        stats.currentBatchSize = 0;
        stats.lastBatchTimestampMs = std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::system_clock::now().time_since_epoch()
        ).count();

        std::cout << "[Batcher] Flushing channel=" << channelId
            << " size=" << batch.messages.size()
            << " totalBatches=" << stats.totalBatchesFlushed
            << std::endl;
        
        // callback (logging, downstream processing) might be slow and should not hold mutex,
        // so batch must be copied first.
        ChannelBatch batchCopy = std::move(batch);

        batchMutex_.unlock();
        try {
            onFlush(batchCopy);
        } catch (const std::exception& e) {
            std::cerr << "[Batcher] Flush callback threw: " << e.what() << std::endl;
        }
        batchMutex_.lock();
    }

    std::optional<BatchStats> MessageBatcher::getChannelStats(const std::string& channelId) const {
        std::unique_lock lock(batchMutex_);

        auto it = stats_.find(channelId);
        if (it == stats_.end()) return std::nullopt;

        BatchStats stats = it->second;
        auto batchIt = batches_.find(channelId);
        if (batchIt != batches_.end()) {
            stats.currentBatchSize = static_cast<int32_t>(batchIt->second.messages.size());
        }

        return stats;
    }

    MessageBatcher::SystemStats MessageBatcher::getSystemStats() const {
        std::unique_lock lock(batchMutex_);

        SystemStats sys;
        sys.activeChannels = static_cast<int32_t>(stats_.size());

        for (const auto& [channelId, stats] : stats_) {
            sys.totalMessages += stats.totalMessages;
            sys.totalBatches += stats.totalBatchesFlushed;
            sys.overallThroughput += stats.messagesPerSecond;
        }

        sys.uptimeSeconds = std::chrono::duration_cast<std::chrono::seconds>(
            std::chrono::steady_clock::now() - startTime_
        ).count();

        return sys;
    }
}