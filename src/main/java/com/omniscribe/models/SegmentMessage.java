package com.omniscribe.models;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SegmentMessage(
        @NotBlank @Size(min = 1, max = 36) String jobId,
        @NotBlank @Size(min = 1, max = 128) String userId,
        @NotNull @Min(0) Integer chunkIndex,
        @NotNull @Min(0) Integer seq,
        @Min(0) Integer startMs,
        @Min(0) Integer endMs,
        @NotBlank String text) {
}
