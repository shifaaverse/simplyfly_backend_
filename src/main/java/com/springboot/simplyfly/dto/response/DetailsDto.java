package com.springboot.simplyfly.dto.response;

import com.springboot.simplyfly.enums.Role;

public record DetailsDto(
        String username,
        boolean isActive,
        Role role
) {
}
