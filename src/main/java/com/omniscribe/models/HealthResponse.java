package com.omniscribe.models;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record HealthResponse(
        String status,
        String db,
        String detail) {

    public HealthResponse(String status, String db) {
        this(status, db, null);
    }
}