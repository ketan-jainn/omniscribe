package com.omniscribe.config;

public final class KafkaTopics {

    private KafkaTopics() {
    }

    public static final String TOPIC_INGRESS = "transcription.ingress";
    public static final String TOPIC_JOBS = "transcription.jobs";
    public static final String TOPIC_SEGMENTS = "transcription.segments";
    public static final String TOPIC_JOBS_RETRY_30S = "transcription.jobs.retry.30s";
    public static final String TOPIC_JOBS_RETRY_5M = "transcription.jobs.retry.5m";
    public static final String TOPIC_DLQ = "transcription.dlq";
}
