#include "ProcessorServiceImpl.hpp";

#include <iostream>

namespace messaging {
    grpc::Status ProcessorServiceImpl::GetChannelStats(
        grpc::ServerContext* /*context*/,
        const messaging::processor::ChannelStatsRequest* request,
        messaging::processor::ChannelStatsResponse* response
    ) {
        const std::string& channelId = request->channel_id();

        if (channelId.empty()) {
            return grpc::Status(
                grpc::StatusCode::INVALID_ARGUMENT,
                "channel_id must not be empty"
            );
        }

        auto stats = batcher_.getChannelStats(channelId);

        if (!stats.has_value()) {
            // channel has never received a message
            // it exists in API Gateway Service, just has no processed messages yet, so don't return NOT_FOUND
            response->set_channel_id(channelId);
            response->set_total_messages(0);
            response->set_messages_per_second(0.0);
            response->set_last_batch_timestamp_ms(0);
            response->set_current_batch_size(0);
            response->set_total_batches_flushed(0);

            return grpc::Status::OK;
        }

        response->set_channel_id(stats->channelId);
        response->set_total_messages(stats->totalMessages);
        response->set_messages_per_second(stats->messagesPerSecond);
        response->set_last_batch_timestamp_ms(stats->lastBatchTimestampMs);
        response->set_current_batch_size(stats->currentBatchSize);
        response->set_total_batches_flushed(stats->totalBatchesFlushed);
 
        return grpc::Status::OK;
    }

    grpc::Status ProcessorServiceImpl::GetSystemStats(
        grpc::ServerContext* /*context*/,
        const messaging::processor::SystemStatsRequest* /*request*/,
        messaging::processor::SystemStatsResponse* response
    ) {
        auto sys = batcher_.getSystemStats();

        response->set_total_messages_processed(sys.totalMessages);
        response->set_overall_throughput_per_second(sys.overallThroughput);
        response->set_active_channels(sys.activeChannels);
        response->set_uptime_seconds(sys.uptimeSeconds);
        response->set_total_batches_flushed(sys.totalBatches);
        response->set_worker_thread_count(batcher_.workerThreadCount());
 
        return grpc::Status::OK;
    }
}