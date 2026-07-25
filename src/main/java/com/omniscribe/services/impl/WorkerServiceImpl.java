package com.omniscribe.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omniscribe.config.KafkaTopics;
import com.omniscribe.dto.SegmentDto;
import com.omniscribe.mappers.SegmentMapper;
import com.omniscribe.messaging.KafkaPublisher;
import com.omniscribe.models.ChunkMessage;
import com.omniscribe.models.Job;
import com.omniscribe.models.Segment;
import com.omniscribe.models.SegmentMessage;
import com.omniscribe.repositories.JobRepository;
import com.omniscribe.repositories.SegmentRepository;
import com.omniscribe.services.WorkerService;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkerServiceImpl implements WorkerService {

    private static final Logger logger = LoggerFactory.getLogger(WorkerServiceImpl.class);
    private final JobRepository jobRepository;
    private final SegmentRepository segmentRepository;
    private final SegmentMapper segmentMapper;
    private final KafkaPublisher kafkaPublisher;
    private final ObjectMapper objectMapper;

    public WorkerServiceImpl(
            JobRepository jobRepository,
            SegmentRepository segmentRepository,
            SegmentMapper segmentMapper,
            KafkaPublisher kafkaPublisher,
            ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.segmentRepository = segmentRepository;
        this.segmentMapper = segmentMapper;
        this.kafkaPublisher = kafkaPublisher;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void processJobMessage(ChunkMessage message) {
        logger.info("Worker received job message: jobId={}, chunkIndex={}, s3Key={}",
                message.jobId(), message.chunkIndex(), message.s3Key());

        Optional<Job> jobOptional = jobRepository.findById(message.jobId());
        if (jobOptional.isEmpty()) {
            logger.warn("Job not found in database for jobId={}", message.jobId());
            return;
        }

        Job job = jobOptional.get();

        // Idempotent segment check & insert via SegmentDto and SegmentMapper
        Optional<Segment> existingSegment = segmentRepository
                .findByJobIdAndChunkIndexAndSeq(job.getId(), message.chunkIndex(), 0);

        if (existingSegment.isEmpty()) {
            SegmentDto segmentDto = new SegmentDto(
                    null,
                    job.getId(),
                    message.chunkIndex(),
                    0,
                    0,
                    1000,
                    "Transcribed segment for chunk " + message.chunkIndex(),
                    Instant.now()
            );

            Segment segment = segmentMapper.toEntity(segmentDto);
            segment.setJob(job);
            segmentRepository.save(segment);
            logger.info("Persisted segment for jobId={}, chunkIndex={}", job.getId(), message.chunkIndex());
        }

        // Publish segment message to transcription.segments topic
        try {
            SegmentMessage segmentMessage = new SegmentMessage(
                    message.jobId(),
                    message.userId(),
                    message.chunkIndex(),
                    0,
                    0,
                    1000,
                    "Transcribed segment for chunk " + message.chunkIndex()
            );
            String payload = objectMapper.writeValueAsString(segmentMessage);
            kafkaPublisher.send(KafkaTopics.TOPIC_SEGMENTS, message.jobId(), payload);
        } catch (Exception e) {
            logger.error("Error publishing segment message", e);
        }
    }

    @KafkaListener(topics = KafkaTopics.TOPIC_JOBS, groupId = "whisper-workers", autoStartup = "${omniscribe.worker.enabled:true}")
    public void onJobMessage(String rawPayload) {
        logger.info("Worker listener received message payload: {}", rawPayload);
        try {
            ChunkMessage message = objectMapper.readValue(rawPayload, ChunkMessage.class);
            processJobMessage(message);
        } catch (Exception e) {
            logger.error("Error parsing job message in worker", e);
        }
    }
}
