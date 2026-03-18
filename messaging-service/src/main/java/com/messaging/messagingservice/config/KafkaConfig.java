package com.messaging.messagingservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.messaging.common.kafka.config.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Producer factory, KafkaTemplate, and topic declarations.
 */
@Configuration
public class KafkaConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    public static final int MESSAGES_INBOUND_PARTITIONS = 6;

    /**
     * Create {@link com.messaging.common.kafka.config.KafkaTopics#MESSAGES_INBOUND inbound messages} topic.
     *
     * @return Configured {@link com.messaging.common.kafka.config.KafkaTopics#MESSAGES_INBOUND inbound messages} topic.
     */
    @Bean
    public NewTopic messagesInboundTopic() {
        return TopicBuilder
                .name(KafkaTopics.MESSAGES_INBOUND)
                .partitions(MESSAGES_INBOUND_PARTITIONS)
                .replicas(1)
                .config("retention.ms", String.valueOf(7 * 24 * 60 * 60 * 1000L))
                .config("compression.type", "lz4")
                .build();
    }

    /**
     * Configures ProducerFactory for serializing Kafka events.
     * <p>
     * Guarantees exactly-once delivery, waits before sending to create batches.
     * </p>
     *
     * @return Configured ProducerFactory
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();

        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // guarantees exactly-once delivery per producer session
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        // producer will wait before sending in order to accumulate more records into a single batch.
        // this will slightly increase latency but significantly increases throughput.
        config.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 32 * 1024); // 32KB batches

        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");

        config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        DefaultKafkaProducerFactory<String, Object> factory = new DefaultKafkaProducerFactory<>(config);
        factory.setValueSerializer(new JsonSerializer<>(objectMapper()));

        return factory;
    }

    /**
     * Primary interface for publishing messages. Thread-safe.
     *
     * @return Configured KafkaTemplate.
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(producerFactory());
        template.setObservationEnabled(true);

        return template;
    }

    /**
     * Shared ObjectMapper configured for Kafka Serializer.
     * <p>
     * Serializes Instant as ISO-8601 string and forces string format for dates.
     * </p>
     *
     * @return Configured ObjectMapper.
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return mapper;
    }
}
