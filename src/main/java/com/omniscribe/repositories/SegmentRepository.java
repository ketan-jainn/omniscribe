package com.omniscribe.repositories;

import com.omniscribe.models.Segment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SegmentRepository extends JpaRepository<Segment, String> {

    List<Segment> findByJobId(String jobId);

    Optional<Segment> findByJobIdAndChunkIndexAndSeq(String jobId, Integer chunkIndex, Integer seq);
}
