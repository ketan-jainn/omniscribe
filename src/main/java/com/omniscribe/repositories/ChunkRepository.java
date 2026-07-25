package com.omniscribe.repositories;

import com.omniscribe.models.Chunk;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChunkRepository extends JpaRepository<Chunk, String> {

    List<Chunk> findByJobId(String jobId);

    Optional<Chunk> findByJobIdAndIndex(String jobId, Integer index);
}
