package com.springboot.simplyfly.service;

import com.springboot.simplyfly.dto.request.PassengerDto;
import com.springboot.simplyfly.dto.request.PassengerUpdateDto;
import com.springboot.simplyfly.dto.response.PassengerRespDto;
import com.springboot.simplyfly.enums.Role;
import com.springboot.simplyfly.exception.InvalidCallException;
import com.springboot.simplyfly.exception.ResourceNotFoundException;
import com.springboot.simplyfly.model.Passenger;
import com.springboot.simplyfly.model.User;
import com.springboot.simplyfly.repository.PassengerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PassengerServiceTest {

    @InjectMocks
    private PassengerService passengerService;

    @Mock
    private PassengerRepository passengerRepository;

    @Mock
    private UserService userService;

    private Passenger getPassenger() {
        User user = new User("harry", "encoded", Role.PASSENGER);
        Passenger passenger = new Passenger();
        passenger.setId(1L);
        passenger.setName("Harry");
        passenger.setEmail("harry@gmail.com");
        passenger.setContact("9876543210");
        passenger.setDateOfBirth(LocalDate.of(2000, 1, 1));
        passenger.setUser(user);
        return passenger;
    }

    @Test
    void insertPassengerTest() {
        User user = new User("harry", "encoded", Role.PASSENGER);
        PassengerDto dto = new PassengerDto("Harry", "harry@gmail.com", "9876543210",
                LocalDate.of(2000, 1, 1), "harry", "harry@123");
        when(userService.getUserObj("harry", "harry@123", Role.PASSENGER)).thenReturn(user);

        passengerService.insertPassenger(dto);

        ArgumentCaptor<Passenger> captor = ArgumentCaptor.forClass(Passenger.class);
        verify(passengerRepository, times(1)).save(captor.capture());
        assertEquals("Harry", captor.getValue().getName());
        assertEquals("harry@gmail.com", captor.getValue().getEmail());
        // user must be attached to the passenger
        assertEquals(user, captor.getValue().getUser());
    }

    @Test
    void insertPassengerTest_UsernameTaken() {
        PassengerDto dto = new PassengerDto("Harry", "harry@gmail.com", "9876543210",
                LocalDate.of(2000, 1, 1), "harry", "harry@123");
        when(userService.getUserObj("harry", "harry@123", Role.PASSENGER))
                .thenThrow(new InvalidCallException("Username already taken"));

        assertThrows(InvalidCallException.class, () -> passengerService.insertPassenger(dto));

        verifyNoInteractions(passengerRepository);
    }

    @Test
    void getByUsernameTest() {
        when(passengerRepository.getByUsername("harry")).thenReturn(getPassenger());

        Passenger passenger = passengerService.getByUsername("harry");

        assertEquals("Harry", passenger.getName());
    }

    @Test
    void getByUsernameTest_NotFound() {
        when(passengerRepository.getByUsername("ghost")).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> passengerService.getByUsername("ghost"));
    }

    @Test
    void getByIdTest() {
        when(passengerRepository.findById(1L)).thenReturn(Optional.of(getPassenger()));

        Passenger passenger = passengerService.getById(1L);

        assertEquals(1L, passenger.getId());
    }

    @Test
    void getByIdTest_InvalidId() {
        when(passengerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> passengerService.getById(99L));
    }

    @Test
    void getProfileTest() {
        when(passengerRepository.getByUsername("harry")).thenReturn(getPassenger());

        PassengerRespDto dto = passengerService.getProfile("harry");

        assertEquals("Harry", dto.name());
        assertEquals("harry", dto.username());
        assertTrue(dto.isActive());
    }

    @Test
    void getProfileTest_NotFound() {
        when(passengerRepository.getByUsername("ghost")).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> passengerService.getProfile("ghost"));
    }

    @Test
    void updateProfileTest() {
        Passenger passenger = getPassenger();
        when(passengerRepository.getByUsername("harry")).thenReturn(passenger);

        passengerService.updateProfile("harry", new PassengerUpdateDto("Harry Potter", "9999999999"));

        assertEquals("Harry Potter", passenger.getName());
        assertEquals("9999999999", passenger.getContact());
        verify(passengerRepository, times(1)).save(passenger);
    }

    @Test
    void updateProfileTest_OnlyContact() {
        Passenger passenger = getPassenger();
        when(passengerRepository.getByUsername("harry")).thenReturn(passenger);

        passengerService.updateProfile("harry", new PassengerUpdateDto(null, "9999999999"));

        // name is untouched
        assertEquals("Harry", passenger.getName());
        assertEquals("9999999999", passenger.getContact());
        verify(passengerRepository, times(1)).save(passenger);
    }

    @Test
    void updateProfileTest_NothingToUpdate() {
        when(passengerRepository.getByUsername("harry")).thenReturn(getPassenger());
        PassengerUpdateDto dto = new PassengerUpdateDto(null, "   ");

        assertThrows(InvalidCallException.class, () -> passengerService.updateProfile("harry", dto));

        verify(passengerRepository, times(0)).save(any(Passenger.class));
    }

    @Test
    void getAllTest() {
        when(passengerRepository.searchPassengers(eq("harry"), any(Pageable.class)))
                .thenReturn(List.of(getPassenger()));

        List<PassengerRespDto> list = passengerService.getAll("harry", 0, 10);

        assertEquals(1, list.size());
        assertEquals("Harry", list.get(0).name());
    }

    @Test
    void getAllTest_BlankSearchListsEveryone() {
        // blank search is sent to the repository as null
        when(passengerRepository.searchPassengers(isNull(), any(Pageable.class)))
                .thenReturn(List.of(getPassenger()));

        List<PassengerRespDto> list = passengerService.getAll("   ", 0, 10);

        assertEquals(1, list.size());
    }

    @Test
    void getAllTest_NoMatch() {
        when(passengerRepository.searchPassengers(eq("zzz"), any(Pageable.class))).thenReturn(List.of());

        List<PassengerRespDto> list = passengerService.getAll("zzz", 0, 10);

        assertTrue(list.isEmpty());
    }

    @Test
    void updateStatusTest() {
        Passenger passenger = getPassenger();
        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));

        passengerService.updateStatus(1L, false);

        verify(userService, times(1)).updateActive(passenger.getUser(), false);
    }

    @Test
    void updateStatusTest_InvalidId() {
        when(passengerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> passengerService.updateStatus(99L, false));

        verifyNoInteractions(userService);
    }
}