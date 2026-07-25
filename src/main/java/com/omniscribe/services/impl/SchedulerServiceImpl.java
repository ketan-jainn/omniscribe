package com.omniscribe.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omniscribe.config.KafkaTopics;
import com.omniscribe.messaging.KafkaPublisher;
import com.omniscribe.models.ChunkMessage;
import com.omniscribe.services.SchedulerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class SchedulerServiceImpl implements SchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerServiceImpl.class);
    private final KafkaPublisher kafkaPublisher;
    private final ObjectMapper objectMapper;

    public SchedulerServiceImpl(KafkaPublisher kafkaPublisher, ObjectMapper objectMapper) {
        this.kafkaPublisher = kafkaPublisher;
        this.objectMapper = objectMapper;
    }

    @Override
    public void processIngressMessage(ChunkMessage message) {
        logger.info("Scheduler passthrough processing message for job_id={}, chunk_index={}",
                message.jobId(), message.chunkIndex());
        try {
            String payload = objectMapper.writeValueAsString(message);
            kafkaPublisher.send(KafkaTopics.TOPIC_JOBS, message.jobId(), payload);
        } catch (JsonProcessingException e) {
            logger.error("Error serializing ChunkMessage for scheduler passthrough", e);
        }
    }

    @KafkaListener(topics = KafkaTopics.TOPIC_INGRESS, groupId = "omniscribe-scheduler", autoStartup = "${omniscribe.scheduler.enabled:true}")
    public void onIngressMessage(String rawPayload) {
        logger.info("Scheduler received raw ingress message: {}", rawPayload);
        try {
            ChunkMessage message = objectMapper.readValue(rawPayload, ChunkMessage.class);
            processIngressMessage(message);
        } catch (Exception e) {
            logger.error("Error parsing ingress message in scheduler", e);
        }
    }
}
