package com.omniscribe.mappers;

import com.omniscribe.dto.ChunkDto;
import com.omniscribe.models.Chunk;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ChunkMapper {

    @Mapping(target = "jobId", source = "job.id")
    ChunkDto toDto(Chunk entity);

    @Mapping(target = "job", ignore = true)
    Chunk toEntity(ChunkDto dto);
}
