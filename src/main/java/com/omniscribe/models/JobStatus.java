package com.omniscribe.models;

public enum JobStatus {
    PENDING_UPLOAD,
    UPLOADED,
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED,
    DLQ
}