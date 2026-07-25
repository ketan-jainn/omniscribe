package com.omniscribe.dto;

import java.time.Instant;

public record ChunkDto(
        String id,
        String jobId,
        Integer index,
        String s3Key,
        String status,
        Integer retryCount,
        Instant createdAt,
        Instant updatedAt) {
}
