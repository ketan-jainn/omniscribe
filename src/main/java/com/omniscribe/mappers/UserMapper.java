package com.omniscribe.mappers;

import com.omniscribe.dto.UserDto;
import com.omniscribe.models.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toDto(User entity);

    User toEntity(UserDto dto);
}
