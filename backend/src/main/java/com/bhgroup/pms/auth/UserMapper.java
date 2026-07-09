package com.bhgroup.pms.auth;

import com.bhgroup.pms.auth.dto.UserResponse;
import com.bhgroup.pms.user.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}
