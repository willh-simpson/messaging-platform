package com.messaging.deliveryservice.config;

import com.messaging.common.kafka.event.MessageCreatedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Consumer configuration for deserializing messages dispatched by Messaging Service.
 * <p>
 * This consumer only retries once. If WebSocket broadcast fails then multiple retries will not fix it.
 * If the consumer fails then too many retires will block real-time delivery.
 * </p>
 */
@Configuration
public class KafkaConsumerConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    public static final String CONSUMER_GROUP_ID = "delivery-consumers";

    /**
     * Configures ConsumerFactory for deserializing incoming messages.
     * Deserializers are wrapped with ErrorHandlingDeserializer to send single malformed messages to error handler.
     *
     * @return Configured ConsumerFactory.
     */
    @Bean
    public ConsumerFactory<String, MessageCreatedEvent> consumerFactory() {
        Map<String, Object> config = new HashMap<>();

        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_GROUP_ID);

        /*
         * setting to 'earliest' could send entire message history to clients,
         * so latest is better for directly receiving the actual real-time messages.
         */
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 50);

        /*
         * if this instance dies then kafka reassigns its partitions to another instance
         * in 10 seconds rather than default 30 seconds.
         * faster fail-over in this instance is better for less message lag.
         */
        config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 10_000);
        config.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3_000);

        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, MessageCreatedEvent.class.getName());
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.messaging.common.kafka.event");

        return new DefaultKafkaConsumerFactory<>(config);
    }

    /**
     * Configures listeners and consumer threads for this service instance.
     * Set up to handle 2 partitions per thread.
     *
     * @return Configured kafka listener container factory.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MessageCreatedEvent>
    kafkaListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, MessageCreatedEvent>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
        return factory;
    }
}
