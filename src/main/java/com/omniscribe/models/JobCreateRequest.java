package com.omniscribe.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record JobCreateRequest(
        @NotBlank String userId,
        @NotEmpty List<@NotBlank String> chunks) {
}