package com.omniscribe.controllers;

import com.omniscribe.dto.JobDto;
import com.omniscribe.mappers.JobMapper;
import com.omniscribe.models.JobCreateRequest;
import com.omniscribe.models.JobResponse;
import com.omniscribe.models.JobResult;
import com.omniscribe.services.JobService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/jobs")
public class JobController {

    private final JobService jobService;
    private final JobMapper jobMapper;

    public JobController(JobService jobService, JobMapper jobMapper) {
        this.jobService = jobService;
        this.jobMapper = jobMapper;
    }

    @PostMapping
    public ResponseEntity<JobResponse> createJob(
            @Valid @RequestBody JobCreateRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        JobResult result = jobService.createJob(request, idempotencyKey);
        HttpStatus status = result.created() ? HttpStatus.ACCEPTED : HttpStatus.OK;
        JobResponse response = jobMapper.toResponse(result.job());
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<?> getJob(@PathVariable String jobId) {
        Optional<JobDto> jobDtoOptional = jobService.getJobById(jobId);
        if (jobDtoOptional.isPresent()) {
            return ResponseEntity.ok(jobMapper.toResponse(jobDtoOptional.get()));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("detail", "Job not found"));
    }
}