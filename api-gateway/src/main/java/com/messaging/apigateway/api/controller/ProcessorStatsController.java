package com.messaging.apigateway.api.controller;

import com.messaging.apigateway.api.dto.response.ProcessorStatsResponse;
import com.messaging.apigateway.config.ApiMediaTypes;
import com.messaging.apigateway.grpc.CppProcessorClient;
import com.messaging.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Exposes endpoints for C++ processor metrics.
 */
@RestController
@RequestMapping("/api/processor")
@RequiredArgsConstructor
public class ProcessorStatsController {
    private final CppProcessorClient processorClient;

    /**
     * Get aggregate stats across all channels being processed.
     *
     * @return 200, system stats.
     */
    @GetMapping(value = "/stats/system", produces = ApiMediaTypes.V1_JSON)
    public ResponseEntity<ApiResponse<ProcessorStatsResponse.SystemStats>> getSystemStats() {
        return processorClient.getSystemStats()
                .map(ProcessorStatsResponse.SystemStats::toSystemStats)
                .map(stats -> ResponseEntity.ok(ApiResponse.success(stats)))
                .orElseGet(() ->
                        ResponseEntity.ok(ApiResponse.success(
                                ProcessorStatsResponse.SystemStats.builder()
                                        .processorAvailable(false)
                                        .build()
                        ))
                );
    }

    /**
     * Get per-channel processing stats for specified channel.
     *
     * @param channelId Desired channel ID.
     * @return 200, system stats.
     */
    @GetMapping(value = "/stats/channels/{channel_id}", produces = ApiMediaTypes.V1_JSON)
    public ResponseEntity<ApiResponse<ProcessorStatsResponse.ChannelStats>> getChannelStats(
            @PathVariable("channel_id") UUID channelId
    ) {
        return processorClient.getChannelStats(channelId.toString())
                .map(ProcessorStatsResponse.ChannelStats::toChannelStats)
                .map(stats -> ResponseEntity.ok(ApiResponse.success(stats)))
                .orElseGet(() ->
                        ResponseEntity.ok(ApiResponse.success(
                                ProcessorStatsResponse.ChannelStats.builder()
                                        .channelId(channelId.toString())
                                        .processorAvailable(false)
                                        .build()
                        ))
                );
    }
}
