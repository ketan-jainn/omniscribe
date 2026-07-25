package com.omniscribe.models;

import com.omniscribe.dto.JobDto;

public record JobResult(
        JobDto job,
        boolean created) {
}