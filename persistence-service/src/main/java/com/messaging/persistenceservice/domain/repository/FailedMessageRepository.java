package com.messaging.persistenceservice.domain.repository;

import com.messaging.persistenceservice.domain.model.FailedMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FailedMessageRepository extends MongoRepository<FailedMessage, UUID> {
    Page<FailedMessage> findByStatusOrderByFailedAtDesc(
            FailedMessage.FailedMessageStatus status,
            Pageable pageable
    );
    Optional<FailedMessage> findByMessageId(UUID messageId);

    long countByStatus(FailedMessage.FailedMessageStatus status);
}
