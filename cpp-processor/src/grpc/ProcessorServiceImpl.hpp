#pragma once

#include "message_processor.grpc.pb.h"
#include "batch/MessageBatcher.hpp"

namespace messaging {
    /**
     * Implements gRPC MessageProcessorService.
     * Receives protobuf requests, queries the batcher's stats, and serializes
     * responses back as protobuf messages.
     */
    class ProcessorServiceImpl final : public messaging::processor::MessageProcessorService::Service {
    public:
        explicit ProcessorServiceImpl(MessageBatcher& batcher): batcher_(batcher) {}

        grpc::Status GetChannelStats(
            grpc::ServerContext* context,
            const messaging::processor::ChannelStatsRequest* request,
            messaging::processor::ChannelStatsResponse* response
        ) override;

        grpc::Status GetSystemStats(
            grpc::ServerContext* context,
            const messaging::processor::SystemStatsRequest* request,
            messaging::processor::SystemStatsResponse* response
        ) override;

    private:
        MessageBatcher& batcher_;
    };
}