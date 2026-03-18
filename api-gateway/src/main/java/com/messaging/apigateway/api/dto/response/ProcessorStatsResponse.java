package com.messaging.apigateway.api.dto.response;

import com.messaging.grpc.processor.ChannelStatsResponse;
import com.messaging.grpc.processor.SystemStatsResponse;
import lombok.Builder;

public record ProcessorStatsResponse() {
    @Builder
    public record ChannelStats(
            String channelId,
            long totalMessages,
            double messagesPerSecond,
            long lastBatchTimestampMs,
            int currentBatchSize,
            long totalBatchesFlushed,
            boolean processorAvailable
    ) {
        public static ChannelStats toChannelStats(ChannelStatsResponse proto) {
            return ChannelStats.builder()
                    .channelId(proto.getChannelId())
                    .totalMessages(proto.getTotalMessages())
                    .messagesPerSecond(proto.getMessagesPerSecond())
                    .lastBatchTimestampMs(proto.getLastBatchTimestampMs())
                    .currentBatchSize(proto.getCurrentBatchSize())
                    .totalBatchesFlushed(proto.getTotalBatchesFlushed())
                    .processorAvailable(true)
                    .build();
        }
    }

    @Builder
    public record SystemStats(
            long totalMessagesProcessed,
            double overallThroughputPerSecond,
            int activeChannels,
            long uptimeSeconds,
            long totalBatchesFlushed,
            int workerThreadCount,
            boolean processorAvailable
    ) {
        public static SystemStats toSystemStats(SystemStatsResponse proto) {
            return SystemStats.builder()
                    .totalMessagesProcessed(proto.getTotalMessagesProcessed())
                    .overallThroughputPerSecond(proto.getOverallThroughputPerSecond())
                    .activeChannels(proto.getActiveChannels())
                    .uptimeSeconds(proto.getUptimeSeconds())
                    .totalBatchesFlushed(proto.getTotalBatchesFlushed())
                    .workerThreadCount(proto.getWorkerThreadCount())
                    .processorAvailable(true)
                    .build();
        }
    }
}
