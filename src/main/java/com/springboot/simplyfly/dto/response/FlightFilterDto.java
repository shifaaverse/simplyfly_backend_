package com.springboot.simplyfly.dto.response;

import java.util.List;

public record FlightFilterDto(
        List<FlightSearchRespDto> list,
        PaginationDto paginationInfo,
        List<String> airlines
) {
}
