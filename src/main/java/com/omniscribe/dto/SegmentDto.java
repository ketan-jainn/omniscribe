package com.omniscribe.dto;

import java.time.Instant;

public record SegmentDto(
        String id,
        String jobId,
        Integer chunkIndex,
        Integer seq,
        Integer startMs,
        Integer endMs,
        String text,
        Instant createdAt) {
}
