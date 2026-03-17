package com.messaging.common.kafka.config;

/**
 * Kafka topic name constants shared across services.
 */
public final class KafkaTopics {
    public static final String MESSAGES_INBOUND = "messages.inbound";
    public static final String MESSAGES_INBOUND_DLT = "messages.inbound-DLT";

    private KafkaTopics() {}
}
