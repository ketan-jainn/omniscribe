package com.omniscribe.mappers;

import com.omniscribe.dto.SegmentDto;
import com.omniscribe.models.Segment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SegmentMapper {

    @Mapping(target = "jobId", source = "job.id")
    SegmentDto toDto(Segment entity);

    @Mapping(target = "job", ignore = true)
    Segment toEntity(SegmentDto dto);
}
