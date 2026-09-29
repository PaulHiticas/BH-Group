package com.bhstays.pms.service.mapper;

import com.bhstays.pms.dto.auth.UserResponse;
import com.bhstays.pms.domain.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}
