package com.omniscribe.services;

import com.omniscribe.dto.JobDto;
import com.omniscribe.models.JobCreateRequest;
import com.omniscribe.models.JobResult;
import java.util.Optional;

public interface JobService {

    JobResult createJob(JobCreateRequest request, String idempotencyKey);

    Optional<JobDto> getJobById(String jobId);
}