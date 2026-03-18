package com.messaging.messagingservice.controller.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.messaging.common.kafka.config.KafkaTopics;
import com.messaging.messagingservice.config.ApiMediaTypes;
import com.messaging.messagingservice.domain.repository.ChannelMemberViewRepository;
import com.messaging.common.kafka.event.MessageCreatedEvent;
import com.messaging.messagingservice.security.AuthenticatedUser;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test:
 * HTTP request -> MessageService -> Kafka broker.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc
@EmbeddedKafka(
        partitions = 6,
        topics = {
                KafkaTopics.MESSAGES_INBOUND
        }
)
@TestPropertySource(
        properties = {
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
        }
)
@DisplayName("MessageController integration tests")
public class MessageControllerIntegrationTest {
        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private EmbeddedKafkaBroker embeddedKafkaBroker;

        @MockBean
        private ChannelMemberViewRepository channelMemberViewRepo;

        private KafkaMessageListenerContainer<String, String> listenerContainer;
        private BlockingQueue<ConsumerRecord<String, String>> consumedRecords;

        private final UUID userId = UUID.randomUUID();
        private final UUID channelId = UUID.randomUUID();

        @BeforeEach
        void setupKafkaConsumer() {
                Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                        "test-group", "true", embeddedKafkaBroker
                );
                consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
                consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
                consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

                var factory = new DefaultKafkaConsumerFactory<String, String>(consumerProps);

                // received records go in the blocking queue so they can be polled with timeout
                consumedRecords = new LinkedBlockingQueue<>();
                var containerProperties = new ContainerProperties(KafkaTopics.MESSAGES_INBOUND);
                containerProperties.setMessageListener(
                        (MessageListener<String, String>) consumedRecords::add
                );

                listenerContainer = new KafkaMessageListenerContainer<>(factory, containerProperties);
                listenerContainer.start();

                // waiting is necessary or else records published before assignment are missed
                ContainerTestUtils.waitForAssignment(
                        listenerContainer, embeddedKafkaBroker.getPartitionsPerTopic()
                );
        }

        @BeforeEach
        void setupSecurityContext() {
                AuthenticatedUser principal = new AuthenticatedUser(userId, "user");
                var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(auth);
        }

        @AfterEach
        void clearSecurityContext() {
                listenerContainer.stop();
                SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("POST /api/messages returns 202, event arrives on Kafka topic")
        void publishMessage_endToEnd_eventArrivesOnKafka() throws Exception {
                when(channelMemberViewRepo.existsByChannelIdAndUserId(channelId, userId))
                        .thenReturn(true);

                var body = Map.of(
                        "channelId", channelId.toString(),
                        "content", "integration test"
                );

                mockMvc.perform(post("/api/messages")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(ApiMediaTypes.V1_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                        .andExpect(status().isAccepted())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                        .andExpect(jsonPath("$.data.messageId").isNotEmpty())
                        .andExpect(jsonPath("$.data.channelId").value(channelId.toString()));

                ConsumerRecord<String, String> received = consumedRecords.poll(5, TimeUnit.SECONDS);

                assertThat(received).as("Expected a record on the Kafka topic but none arrived").isNotNull();
                assertThat(received.key()).isEqualTo(channelId.toString());

                MessageCreatedEvent event = objectMapper.readValue(received.value(), MessageCreatedEvent.class);

                assertThat(event.getChannelId()).isEqualTo(channelId);
                assertThat(event.getAuthorId()).isEqualTo(userId);
                assertThat(event.getAuthorUsername()).isEqualTo("user");
                assertThat(event.getContent()).isEqualTo("integration test");
                assertThat(event.getEventType()).isEqualTo("MESSAGE_CREATED");
                assertThat(event.getMessageId()).isNotNull();
                assertThat(event.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("POST /api/messages returns 403 when user is not a channel member")
        void publishMessage_notMember_returns403() throws Exception {
                when(channelMemberViewRepo.existsByChannelIdAndUserId(channelId, userId))
                        .thenReturn(false);

                var body = Map.of(
                        "channelId", channelId.toString(),
                        "content", "Should be rejected"
                );

                mockMvc.perform(post("/api/messages")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(ApiMediaTypes.V1_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.success").value(false));

                // Verify nothing was published to Kafka
                ConsumerRecord<String, String> received = consumedRecords.poll(1, TimeUnit.SECONDS);
                assertThat(received).isNull();
        }

        @Test
        @DisplayName("POST /api/messages returns 406 when Accept header is missing")
        void publishMessage_noVersionHeader_returns406() throws Exception {
                var body = Map.of("channelId", channelId.toString(), "content", "test");

                mockMvc.perform(post("/api/messages")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                        .andExpect(status().isNotAcceptable());
        }
}
