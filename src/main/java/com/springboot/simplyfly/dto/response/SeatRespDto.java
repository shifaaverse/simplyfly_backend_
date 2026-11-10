package com.springboot.simplyfly.dto.response;

import com.springboot.simplyfly.enums.SeatClass;
import com.springboot.simplyfly.enums.SeatPosition;
import com.springboot.simplyfly.enums.SeatStatus;

public record SeatRespDto(
        Long seatId,
        String seatNumber,
        SeatClass seatClass,
        SeatPosition seatPosition,
        SeatStatus status,
        double extraCharge
) {
}
