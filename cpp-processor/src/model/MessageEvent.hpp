#pragma once

#include <string>
#include <chrono>
#include <nlohmann/json.hpp>

namespace messaging {
    /**
     * C++ representation of MessageCreatedEvent.java.
     */
    struct MessageEvent {
        std::string messageId;

        std::string channelId;
        std::string authorId;
        std::string authorUsername;
        std::string content;

        std::string createdAt; // represented as ISO-8601 string
        std::string eventType;
        int schemaVersion{1};

        static MessageEvent fromJson(const nlohmann::json& j) {
            MessageEvent e;

            e.messageId = j.value("message_id", "");
            e.channelId = j.value("channel_id", "");
            e.authorId = j.value("author_id", "");
            e.authorUsername = j.value("author_username", "");
            e.content = j.value("content", "");
            e.createdAt = j.value("created_at", "");
            e.eventType = j.value("event_type", "MESSAGE_CREATED");
            e.schemaVersion = j.value("schema_version", 1);

            return e;
        }
    };
}