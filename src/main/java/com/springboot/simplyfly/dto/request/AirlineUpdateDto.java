package com.springboot.simplyfly.dto.request;

public record AirlineUpdateDto(
        String email,
        String contact,
        String address,
        String logoUrl
) {
}
