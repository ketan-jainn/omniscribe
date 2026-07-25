package com.omniscribe.services.impl;

import com.omniscribe.dto.JobDto;
import com.omniscribe.mappers.JobMapper;
import com.omniscribe.models.Job;
import com.omniscribe.models.JobCreateRequest;
import com.omniscribe.models.JobResult;
import com.omniscribe.models.JobStatus;
import com.omniscribe.repositories.JobRepository;
import com.omniscribe.services.JobService;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final JobMapper jobMapper;

    public JobServiceImpl(JobRepository jobRepository, JobMapper jobMapper) {
        this.jobRepository = jobRepository;
        this.jobMapper = jobMapper;
    }

    @Override
    @Transactional
    public JobResult createJob(JobCreateRequest request, String idempotencyKey) {
        Optional<Job> existingJob = jobRepository.findByIdempotencyKey(idempotencyKey);
        if (existingJob.isPresent()) {
            return new JobResult(jobMapper.toDto(existingJob.get()), false);
        }

        Job job = new Job();
        job.setUserId(request.userId());
        job.setStatus(JobStatus.PENDING_UPLOAD);
        job.setIdempotencyKey(idempotencyKey);
        job.setChunkCount(request.chunks() != null ? request.chunks().size() : 0);
        Job savedJob = jobRepository.save(job);

        return new JobResult(jobMapper.toDto(savedJob), true);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<JobDto> getJobById(String jobId) {
        return jobRepository.findById(jobId).map(jobMapper::toDto);
    }
}