package com.messaging.persistenceservice.infrastructure.config;

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
 */
@Configuration
public class KafkaConsumerConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    public static final String CONSUMER_GROUP_ID = "persistence-consumers";

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
         * starting consumer group from the oldest available message on the partition
         * ensures no messages are missed on first deployment.
         * this is necessary for accurate message history persistence.
         */
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // forcing offset to only advance after message is successfully processed to ensure scaled persistence
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);

        /*
         * wrapping JsonDeserializer and StringDeserializer with ErrorHandlingDeserializer
         * so that one malformed message doesn't interrupt entire partition.
         */
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

        // producer doesn't add type headers, so deserialized class target needs to be specified here
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
    public ConcurrentKafkaListenerContainerFactory<String, MessageCreatedEvent> kafkaListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, MessageCreatedEvent>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);

        return factory;
    }
}
