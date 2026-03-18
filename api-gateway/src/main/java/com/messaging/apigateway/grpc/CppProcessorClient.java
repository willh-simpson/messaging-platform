package com.messaging.apigateway.grpc;

import com.messaging.grpc.processor.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Spring bean that wraps the generated gRPC stub.
 */
@Component
@Slf4j
public class CppProcessorClient {
    private final ManagedChannel channel;
    private final MessageProcessorServiceGrpc.MessageProcessorServiceBlockingStub stub;

    public CppProcessorClient(
            @Value("${app.cpp-processor.host}") String host,
            @Value("${app.cpp-processor.port}") int port
    ) {
        this.channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();
        this.stub = MessageProcessorServiceGrpc.newBlockingStub(channel);

        log.info("gRPC channel established to cpp-processor at {}:{}", host, port);
    }

    /**
     * Get per-channel stats from the C++ processor.
     *
     * @param channelId Specified channel ID.
     * @return Channel stats, empty if processor is unavailable or channel is unknown.
     */
    public Optional<ChannelStatsResponse> getChannelStats(String channelId) {
        try {
            ChannelStatsResponse response = stub
                    .withDeadlineAfter(2, TimeUnit.SECONDS)
                    .getChannelStats(
                            ChannelStatsRequest.newBuilder()
                                    .setChannelId(channelId)
                                    .build()
                    );

            return Optional.of(response);
        } catch (StatusRuntimeException e) {
            log.warn(
                    "GetChannelStats failed for channel={}: {} {}",
                    channelId, e.getStatus().getCode(), e.getStatus().getDescription()
            );

            return Optional.empty();
        }
    }

    /**
     * Get system-wide stats from C++ processor.
     *
     * @return System stats, empty if processor is unavailable.
     */
    public Optional<SystemStatsResponse> getSystemStats() {
        try {
            SystemStatsResponse response = stub
                    .withDeadlineAfter(2, TimeUnit.SECONDS)
                    .getSystemStats(
                            SystemStatsRequest.newBuilder().build()
                    );

            return Optional.of(response);
        } catch (StatusRuntimeException e) {
            log.warn("GetSystemStats failed: {} {}", e.getStatus().getCode(), e.getStatus().getDescription());

            return Optional.empty();
        }
    }

    /**
     * Cleanly shutdown gRPC channel on application shutdown.
     * Allows in-flight RPCs to complete for up to 5 seconds before force-closing.
     */
    @PreDestroy
    public void shutdown() {
        try {
            channel
                    .shutdown()
                    .awaitTermination(5, TimeUnit.SECONDS);
            log.info("gRPC channel to cpp-processor shut down");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            channel.shutdownNow();
        }
    }
}
