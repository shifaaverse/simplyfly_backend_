package com.springboot.simplyfly.mapper;

import com.springboot.simplyfly.dto.request.ComplaintDto;
import com.springboot.simplyfly.dto.response.ComplaintRespDto;
import com.springboot.simplyfly.model.Complaint;

public class ComplaintMapper {

    private ComplaintMapper() {
    }

    public static Complaint convertDtoToEntity(ComplaintDto dto) {
        Complaint complaint = new Complaint();
        complaint.setCategory(dto.category());
        complaint.setSubject(dto.subject());
        complaint.setDescription(dto.description());
        return complaint;
    }

    public static ComplaintRespDto convertEntityToDto(Complaint complaint) {
        return new ComplaintRespDto(
                complaint.getId(),
                complaint.getPassenger().getName(),
                complaint.getAirline() == null ? null : complaint.getAirline().getName(),
                complaint.getBooking() == null ? null : complaint.getBooking().getPnr(),
                complaint.getCategory(),
                complaint.getSubject(),
                complaint.getDescription(),
                complaint.getStatus(),
                complaint.getAirlineResponse(),
                complaint.getCreatedAt()
        );
    }
}
