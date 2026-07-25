package com.omniscribe.dto;

import com.omniscribe.models.JobStatus;
import java.time.Instant;

public record JobDto(
        String id,
        String userId,
        JobStatus status,
        String s3Prefix,
        Integer chunkCount,
        String idempotencyKey,
        Instant createdAt,
        Instant updatedAt) {
}
