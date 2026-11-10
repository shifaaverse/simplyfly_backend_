package com.springboot.simplyfly.dto.response;

public record PaginationDto(
        int totalPages,
        Long totalRecords,
        Integer numRecordsInPage
) {
}
