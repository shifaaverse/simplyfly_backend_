package com.springboot.simplyfly.utility;

import com.springboot.simplyfly.enums.SeatClass;
import com.springboot.simplyfly.enums.SeatPosition;
import com.springboot.simplyfly.enums.SeatStatus;
import com.springboot.simplyfly.model.Flight;
import com.springboot.simplyfly.model.FlightInstance;
import com.springboot.simplyfly.model.FlightSeat;

import java.util.ArrayList;
import java.util.List;

public class SeatLayoutUtility {

    private static final char[] COLUMNS = {'A', 'B', 'C', 'D', 'E', 'F'};

    private SeatLayoutUtility() {
    }

    // Get the seat position based on its column.
    public static SeatPosition getPosition(char column) {
        return switch (column) {
            case 'A', 'F' -> SeatPosition.WINDOW;
            case 'C', 'D' -> SeatPosition.AISLE;
            default -> SeatPosition.MIDDLE;
        };
    }

    // Generate seats for all classes of a flight instance.
    public static List<FlightSeat> generateSeats(Flight flight, FlightInstance instance) {
        List<FlightSeat> seats = new ArrayList<>();
        int row = 1;

        row = addRows(seats, instance, row, flight.getBusinessRows(), SeatClass.BUSINESS);
        row = addRows(seats, instance, row, flight.getPremiumRows(), SeatClass.PREMIUM_ECONOMY);
        addRows(seats, instance, row, flight.getEconomyRows(), SeatClass.ECONOMY);

        return seats;
    }

    // Create six seats for each row in the selected seat class.
    private static int addRows(List<FlightSeat> seats, FlightInstance instance,
                               int startRow, int numberOfRows, SeatClass seatClass) {
        int row = startRow;

        for (int i = 0; i < numberOfRows; i++) {
            for (char column : COLUMNS) {
                FlightSeat seat = new FlightSeat();
                seat.setFlightInstance(instance);
                seat.setSeatNumber(row + String.valueOf(column));
                seat.setSeatClass(seatClass);
                seat.setSeatPosition(getPosition(column));
                seat.setStatus(SeatStatus.AVAILABLE);
                seats.add(seat);
            }
            row++;
        }

        return row;
    }
}