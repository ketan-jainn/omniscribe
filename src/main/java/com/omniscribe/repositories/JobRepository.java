package com.omniscribe.repositories;

import com.omniscribe.models.Job;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, String> {

    Optional<Job> findByIdempotencyKey(String idempotencyKey);
}