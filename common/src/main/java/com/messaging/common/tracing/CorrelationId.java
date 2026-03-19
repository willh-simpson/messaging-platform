package com.messaging.common.tracing;

/**
 * Constants for distributed request tracing.
 */
public final class CorrelationId {
    public static final String HEADER_NAME = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";
    public static final String KAFKA_HEADER_KEY = "correlationId";

    private CorrelationId() {}
}
