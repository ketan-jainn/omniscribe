package com.omniscribe.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaPublisher {

    private static final Logger logger = LoggerFactory.getLogger(KafkaPublisher.class);
    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaPublisher(ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider) {
        this.kafkaTemplate = kafkaTemplateProvider.getIfAvailable();
    }

    public void send(String topic, String key, String payload) {
        if (kafkaTemplate == null) {
            logger.info("[Mock/Disabled Kafka] Sending to topic={}, key={}: {}", topic, key, payload);
            return;
        }
        logger.info("Publishing message to topic={}, key={}", topic, key);
        kafkaTemplate.send(topic, key, payload);
    }

    public void send(String topic, String payload) {
        if (kafkaTemplate == null) {
            logger.info("[Mock/Disabled Kafka] Sending to topic={}: {}", topic, payload);
            return;
        }
        logger.info("Publishing message to topic={}", topic);
        kafkaTemplate.send(topic, payload);
    }
}
