package com.omniscribe.mappers;

import com.omniscribe.dto.JobDto;
import com.omniscribe.models.Job;
import com.omniscribe.models.JobResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface JobMapper {

    JobDto toDto(Job entity);

    Job toEntity(JobDto dto);

    @Mapping(target = "jobId", source = "id")
    @Mapping(target = "status", expression = "java(dto.status() != null ? dto.status().name() : null)")
    JobResponse toResponse(JobDto dto);
}
