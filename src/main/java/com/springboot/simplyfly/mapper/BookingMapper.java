package com.springboot.simplyfly.mapper;

import com.springboot.simplyfly.dto.response.BookingRespDto;
import com.springboot.simplyfly.dto.response.JourneyRespDto;
import com.springboot.simplyfly.dto.response.TravellerSeatRespDto;
import com.springboot.simplyfly.enums.MealOption;
import com.springboot.simplyfly.model.Booking;
import com.springboot.simplyfly.model.BookingJourney;
import com.springboot.simplyfly.model.Flight;
import com.springboot.simplyfly.model.TravellerSeat;

import java.util.List;

public class BookingMapper {

    private BookingMapper() {
    }

    // Old bookings have no fare details (null), so the whole fare is shown as the base fare.
    private static double orZero(Double value) {
        return value == null ? 0 : value;
    }

    public static TravellerSeatRespDto convertTravellerSeatToDto(TravellerSeat ts) {
        boolean hasDetails = ts.getBaseFare() != null;
        double baseFare = hasDetails ? ts.getBaseFare() : ts.getFare();

        // Tax is what is left after the charges (before tax) are taken out of the fare
        double tax = Math.round((ts.getFare() - baseFare - orZero(ts.getSeatCharge())
                - orZero(ts.getMealCharge()) - orZero(ts.getBaggageCharge())) * 100.0) / 100.0;

        return new TravellerSeatRespDto(
                ts.getTraveller().getName(),
                ts.getTraveller().getCategory(),
                ts.getFlightSeat() == null ? "Lap infant" : ts.getFlightSeat().getSeatNumber(),
                ts.getFlightSeat() == null ? null : ts.getFlightSeat().getSeatPosition(),
                ts.getTraveller().getMealOption() == null ? MealOption.NONE : ts.getTraveller().getMealOption(),
                ts.getTraveller().getExtraBaggageKg() == null ? 0 : ts.getTraveller().getExtraBaggageKg(),
                baseFare,
                orZero(ts.getSeatCharge()),
                orZero(ts.getMealCharge()),
                orZero(ts.getBaggageCharge()),
                tax,
                ts.getFare()
        );
    }

    public static JourneyRespDto convertJourneyToDto(BookingJourney journey, List<TravellerSeat> seats) {
        Flight flight = journey.getFlightInstance().getFlight();
        return new JourneyRespDto(
                journey.getId(),
                journey.getJourneyType(),
                flight.getAirline().getName(),
                flight.getFlightNumber(),
                flight.getRoute().getSource().getCity(),
                flight.getRoute().getDestination().getCity(),
                journey.getFlightInstance().getTravelDate(),
                flight.getDepartureTime(),
                flight.getArrivalTime(),
                journey.getSeatClass(),
                journey.getAmount(),
                journey.getStatus(),
                journey.getFlightInstance().getStatus(),
                seats.stream().map(BookingMapper::convertTravellerSeatToDto).toList()
        );
    }

    public static BookingRespDto convertEntityToDto(Booking booking, List<JourneyRespDto> journeys) {
        return new BookingRespDto(
                booking.getId(),
                booking.getPnr(),
                booking.getTripType(),
                booking.getStatus(),
                booking.getTotalAmount(),
                booking.getCreatedAt(),
                journeys
        );
    }
}
