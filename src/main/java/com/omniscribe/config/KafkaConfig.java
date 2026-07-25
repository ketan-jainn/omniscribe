package com.omniscribe.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@Profile("!test")
public class KafkaConfig {

    @Bean
    public NewTopic ingressTopic() {
        return TopicBuilder.name(KafkaTopics.TOPIC_INGRESS)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic jobsTopic() {
        return TopicBuilder.name(KafkaTopics.TOPIC_JOBS)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic segmentsTopic() {
        return TopicBuilder.name(KafkaTopics.TOPIC_SEGMENTS)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic retry30sTopic() {
        return TopicBuilder.name(KafkaTopics.TOPIC_JOBS_RETRY_30S)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic retry5mTopic() {
        return TopicBuilder.name(KafkaTopics.TOPIC_JOBS_RETRY_5M)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic dlqTopic() {
        return TopicBuilder.name(KafkaTopics.TOPIC_DLQ)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
