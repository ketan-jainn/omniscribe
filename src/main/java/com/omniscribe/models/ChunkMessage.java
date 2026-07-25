package com.omniscribe.models;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChunkMessage(
        @NotBlank @Size(min = 1, max = 36) String jobId,
        @NotBlank @Size(min = 1, max = 128) String userId,
        @NotNull @Min(0) Integer chunkIndex,
        @NotBlank @Size(min = 1, max = 1024) String s3Key,
        @NotNull @Min(0) Integer retryCount) {

    public ChunkMessage(String jobId, String userId, Integer chunkIndex, String s3Key) {
        this(jobId, userId, chunkIndex, s3Key, 0);
    }
}
