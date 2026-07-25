package com.omniscribe.models;

import java.time.Instant;

public record JobResponse(
        String jobId,
        String status,
        String userId,
        Instant createdAt,
        String idempotencyKey) {
}