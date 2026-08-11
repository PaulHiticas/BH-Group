package com.bhgroup.pms.service.mapper;

import com.bhgroup.pms.dto.auth.UserResponse;
import com.bhgroup.pms.domain.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}
