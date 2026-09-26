package com.lavander.estore.dto;

import com.lavander.estore.model.Role;
import com.lavander.estore.model.User;

public record UserDto(Long id, String email, String fullName, Role role) {
    public static UserDto fromEntity(User entity) {
        return new UserDto(entity.getId(), entity.getEmail(), entity.getFullName(), entity.getRole());
    }
}
