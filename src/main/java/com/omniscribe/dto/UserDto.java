package com.omniscribe.dto;

import java.time.Instant;

public record UserDto(
        String id,
        String plan,
        String rateLimitTier,
        Instant createdAt,
        Instant updatedAt) {
}
