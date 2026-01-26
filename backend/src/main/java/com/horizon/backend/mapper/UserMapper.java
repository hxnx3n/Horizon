package com.horizon.backend.mapper;

import com.horizon.backend.dto.auth.LoginResponse;
import com.horizon.backend.dto.user.UserDto;
import com.horizon.backend.dto.user.UserCreateRequest;
import com.horizon.backend.dto.user.UserUpdateRequest;
import com.horizon.backend.entity.User;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "role", source = "role", qualifiedByName = "roleToString")
    UserDto toDto(User user);

    List<UserDto> toDtoList(List<User> users);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "enabled", constant = "true")
    User toEntity(UserCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    void updateEntityFromDto(UserUpdateRequest request, @MappingTarget User user);

    @Mapping(target = "role", source = "role", qualifiedByName = "roleToString")
    LoginResponse.UserInfo toUserInfo(User user);

    @Named("roleToString")
    default String roleToString(User.Role role) {
        return role != null ? role.name() : null;
    }

    @Named("stringToRole")
    default User.Role stringToRole(String role) {
        return role != null ? User.Role.valueOf(role) : null;
    }
}
