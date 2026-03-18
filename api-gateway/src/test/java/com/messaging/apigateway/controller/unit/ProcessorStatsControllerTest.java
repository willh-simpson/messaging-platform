package com.messaging.apigateway.controller.unit;

import com.messaging.apigateway.api.controller.ProcessorStatsController;
import com.messaging.apigateway.api.dto.response.ProcessorStatsResponse;
import com.messaging.apigateway.grpc.CppProcessorClient;
import com.messaging.grpc.processor.ChannelStatsResponse;
import com.messaging.grpc.processor.SystemStatsResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ProcessorStatsController.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessorStatsController")
class ProcessorStatsControllerTest {

    @Mock
    private CppProcessorClient processorClient;

    @InjectMocks
    private ProcessorStatsController controller;

    private final UUID channelId = UUID.randomUUID();

    @Test
    @DisplayName("returns system stats when processor is available")
    void getSystemStats_processorAvailable_returnsStats() {
        var proto = SystemStatsResponse.newBuilder()
                .setTotalMessagesProcessed(1000L)
                .setOverallThroughputPerSecond(42.5)
                .setActiveChannels(3)
                .setUptimeSeconds(300L)
                .setTotalBatchesFlushed(50L)
                .setWorkerThreadCount(4)
                .build();

        when(processorClient.getSystemStats()).thenReturn(Optional.of(proto));

        ResponseEntity<?> response = controller.getSystemStats();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        var body = response.getBody();
        assertThat(body).isNotNull();

        // Unwrap ApiResponse.data
        var apiResponse = (com.messaging.common.dto.ApiResponse<?>) body;
        var stats = (ProcessorStatsResponse.SystemStats)
                apiResponse.getData();

        assertThat(stats.totalMessagesProcessed()).isEqualTo(1000L);
        assertThat(stats.overallThroughputPerSecond()).isEqualTo(42.5);
        assertThat(stats.activeChannels()).isEqualTo(3);
        assertThat(stats.uptimeSeconds()).isEqualTo(300L);
        assertThat(stats.totalBatchesFlushed()).isEqualTo(50L);
        assertThat(stats.workerThreadCount()).isEqualTo(4);
        assertThat(stats.processorAvailable()).isTrue();
    }

    @Test
    @DisplayName("returns 200 with processorAvailable=false when processor is down")
    void getSystemStats_processorUnavailable_degradesGracefully() {
        when(processorClient.getSystemStats()).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.getSystemStats();

        // Must be 200 — NOT 500, NOT 503
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        var apiResponse = (com.messaging.common.dto.ApiResponse<?>) response.getBody();
        assertThat(apiResponse).isNotNull();
        assertThat(apiResponse.isSuccess()).isTrue();

        var stats = (ProcessorStatsResponse.SystemStats)
                apiResponse.getData();
        assertThat(stats.processorAvailable()).isFalse();
        assertThat(stats.totalMessagesProcessed()).isZero();
    }

    @Test
    @DisplayName("returns channel stats when processor is available")
    void getChannelStats_processorAvailable_returnsStats() {
        var proto = ChannelStatsResponse.newBuilder()
                .setChannelId(channelId.toString())
                .setTotalMessages(250L)
                .setMessagesPerSecond(12.3)
                .setCurrentBatchSize(7)
                .setTotalBatchesFlushed(10L)
                .setLastBatchTimestampMs(1720000000000L)
                .build();

        when(processorClient.getChannelStats(channelId.toString()))
                .thenReturn(Optional.of(proto));

        ResponseEntity<?> response = controller.getChannelStats(channelId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        var apiResponse = (com.messaging.common.dto.ApiResponse<?>) response.getBody();
        var stats = (ProcessorStatsResponse.ChannelStats)
                apiResponse.getData();

        assertThat(stats.channelId()).isEqualTo(channelId.toString());
        assertThat(stats.totalMessages()).isEqualTo(250L);
        assertThat(stats.messagesPerSecond()).isEqualTo(12.3);
        assertThat(stats.currentBatchSize()).isEqualTo(7);
        assertThat(stats.processorAvailable()).isTrue();
    }

    @Test
    @DisplayName("returns 200 with processorAvailable=false for channel stats when down")
    void getChannelStats_processorUnavailable_degradesGracefully() {
        when(processorClient.getChannelStats(channelId.toString()))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.getChannelStats(channelId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        var apiResponse = (com.messaging.common.dto.ApiResponse<?>) response.getBody();
        var stats = (ProcessorStatsResponse.ChannelStats)
                apiResponse.getData();

        assertThat(stats.processorAvailable()).isFalse();
        assertThat(stats.channelId()).isEqualTo(channelId.toString());
    }
}
