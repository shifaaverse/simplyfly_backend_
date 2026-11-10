package com.springboot.simplyfly.service;

import com.springboot.simplyfly.dto.response.AdminStatDto;
import com.springboot.simplyfly.dto.response.AirlineBookingCountDto;
import com.springboot.simplyfly.enums.AirlineStatus;
import com.springboot.simplyfly.enums.PaymentStatus;
import com.springboot.simplyfly.enums.RefundStatus;
import com.springboot.simplyfly.repository.AirlineRepository;
import com.springboot.simplyfly.repository.BookingRepository;
import com.springboot.simplyfly.repository.FlightRepository;
import com.springboot.simplyfly.repository.PassengerRepository;
import com.springboot.simplyfly.repository.PaymentRepository;
import com.springboot.simplyfly.repository.RefundRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @InjectMocks
    private AdminService adminService;

    @Mock
    private PassengerRepository passengerRepository;

    @Mock
    private AirlineRepository airlineRepository;

    @Mock
    private FlightRepository flightRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RefundRepository refundRepository;

    @Test
    void getStatsTest() {
        when(passengerRepository.count()).thenReturn(10L);
        when(airlineRepository.count()).thenReturn(5L);
        when(airlineRepository.countByStatus(AirlineStatus.APPROVED)).thenReturn(3L);
        when(airlineRepository.countByStatus(AirlineStatus.PENDING)).thenReturn(2L);
        when(flightRepository.count()).thenReturn(8L);
        when(bookingRepository.countByStatusIn(anyList())).thenReturn(20L);
        when(paymentRepository.countByStatus(PaymentStatus.SUCCESS)).thenReturn(18L);
        when(refundRepository.countByStatusIn(anyList())).thenReturn(4L);
        when(paymentRepository.sumAmountByStatus(PaymentStatus.SUCCESS)).thenReturn(50000.0);
        when(refundRepository.sumRefundAmountByStatus(RefundStatus.REFUNDED)).thenReturn(8000.5);

        AdminStatDto dto = adminService.getStats();

        assertEquals(10, dto.totalPassengers());
        assertEquals(5, dto.totalAirlines());
        assertEquals(3, dto.activeAirlines());
        assertEquals(2, dto.pendingAirlines());
        assertEquals(8, dto.totalFlights());
        assertEquals(20, dto.totalBookings());
        assertEquals(18, dto.totalPayments());
        assertEquals(4, dto.pendingRefunds());
        // revenue = collected - refunded
        assertEquals(41999.5, dto.revenue());
    }

    @Test
    void getStatsTest_NoData() {
        // nothing is stubbed, every count / sum is 0
        AdminStatDto dto = adminService.getStats();

        assertEquals(0, dto.totalPassengers());
        assertEquals(0, dto.totalAirlines());
        assertEquals(0, dto.totalBookings());
        assertEquals(0, dto.pendingRefunds());
        assertEquals(0.0, dto.revenue());
    }

    @Test
    void getAirlineWiseBookingCountTest() {
        when(airlineRepository.getAirlineWiseBookingCount(anyList()))
                .thenReturn(List.of(new AirlineBookingCountDto("IndiGo", 34L),
                        new AirlineBookingCountDto("Air India", 12L)));

        List<AirlineBookingCountDto> list = adminService.getAirlineWiseBookingCount();

        assertEquals(2, list.size());
        assertEquals("IndiGo", list.get(0).airlineName());
        assertEquals(34L, list.get(0).numberOfBookings());
        verify(airlineRepository, times(1)).getAirlineWiseBookingCount(anyList());
    }

    @Test
    void getAirlineWiseBookingCountTest_NoBookings() {
        when(airlineRepository.getAirlineWiseBookingCount(anyList())).thenReturn(List.of());

        List<AirlineBookingCountDto> list = adminService.getAirlineWiseBookingCount();

        assertTrue(list.isEmpty());
    }
}