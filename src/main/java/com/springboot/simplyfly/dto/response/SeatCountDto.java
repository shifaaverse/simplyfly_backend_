package com.springboot.simplyfly.dto.response;

public record SeatCountDto(
        Long instanceId,
        Long availableSeats
) {
}
