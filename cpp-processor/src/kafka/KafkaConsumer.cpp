#include "KafkaConsumer.hpp"

#include <iostream>
#include <stdexcept>
#include <thread>
#include <chrono>
#include <nlohmann/json.hpp>

namespace messaging {
    KafkaConsumer::KafkaConsumer(
        const std::string& brokers,
        const std::string& groupId,
        const std::string& topic
    ): brokers_(brokers), groupId_(groupId), topic_(topic) {
        std::string errstr;

        auto conf = std::unique_ptr<RdKafka::Conf>(
            RdKafka::Conf::create(RdKafka::Conf::CONF_GLOBAL)
        );

        conf->set("bootstrap.servers", brokers_, errstr);
        conf->set("group.id", groupId_, errstr);

        // set to "latest" because this is a real-time pipeline component and not a historical replay service
        conf->set("auto.offset.reset", "latest", errstr);
        // prevent offset advancement on messages that failed to batch
        conf->set("enable.auto.commit", "false", errstr);

        conf->set("fetch.message.max.bytes", "10485760", errstr); // 10MB
        conf->set("max.poll.interval.ms", "60000", errstr);

        consumer_.reset(RdKafka::KafkaConsumer::create(conf.get(), errstr));
        if (!consumer_) {
            throw std::runtime_error("Failed to create Kafka consumer: " + errstr);
        }

        RdKafka::ErrorCode err = consumer_->subscribe({topic_});
        if (err != RdKafka::ERR_NO_ERROR) {
            throw std::runtime_error(
                "Failed to subscribe to topic " + topic_ + ": " + RdKafka::err2str(err)
            );
        }

        std::cout << "[KafkaConsumer] Subscribed to " << topic_ << " as group " << groupId_ << std::endl;
    }

    KafkaConsumer::~KafkaConsumer() {
        stop();

        if (consumer_) {
            consumer_->close();
        }
    }

    void KafkaConsumer::start(MessageCallback onMessage) {
        running_.store(true);

        consumerThread_ = std::thread([this, cb = std::move(onMessage)]() {
            consumeLoop(std::move(cb));
        });
    }

    void KafkaConsumer::stop() {
        running_.store(false);

        if (consumerThread_.joinable()) {
            consumerThread_.join();
        }
    }

    void KafkaConsumer::consumeLoop(MessageCallback onMessage) {
        std::cout << "[KafkaConsumer] Consumer loop started" << std::endl;

        while (running_.load()) {
            // maximum batch flush latency. main thread checks batch timeouts between polls
            std::unique_ptr<RdKafka::Message> msg(consumer_->consume(100));

            switch (msg->err()) {
                case RdKafka::ERR_NO_ERROR: {
                    const auto* payload = static_cast<const char*>(msg->payload());
                    std::string payloadStr(payload, msg->len());

                    MessageEvent event;
                    if (tryDeserialize(payloadStr, event)) {
                        onMessage(event);

                        // avoid blocking the consumer loop when committing offset
                        consumer_->commitAsync(msg.get());
                    } else {
                        std::cerr << "[KafkaConsumer] Deserialization failed for "
                            << "partition=" << msg->partition()
                            << " offset=" << msg->offset()
                            << std::endl;
                        
                        // malformed messages should be skipped
                        consumer_->commitAsync(msg.get());
                    }

                    break;
                }

                case RdKafka::ERR__TIMED_OUT:
                    break; // no message in poll window so just continue
                
                case RdKafka::ERR__PARTITION_EOF:
                    break; // reached end of partition. normal for low traffic
                
                default:
                    std::cerr << "[KafkaConsumer] Consumer error: "
                        << msg->errstr()
                        << std::endl;
                    
                    std::this_thread::sleep_for(std::chrono::milliseconds(500));

                    break;
            }
        }

        std::cout << "[KafkaConsumer] Consumer loop stopped" << std::endl;
    }

    bool KafkaConsumer::tryDeserialize(
        const std::string& payload,
        MessageEvent& out
    ) const {
        try {
            auto json = nlohmann::json::parse(payload);
            out = MessageEvent::fromJson(json);

            return !out.messageId.empty() && !out.channelId.empty();
        } catch (const nlohmann::json::exception& e) {
            std::cerr << "[KafkaConsumer] JSON parse error: "
                << e.what()
                << std::endl;

            return false;
        }
    }
}