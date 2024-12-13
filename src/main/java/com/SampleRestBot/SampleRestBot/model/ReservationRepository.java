package com.SampleRestBot.SampleRestBot.model;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByDateAndTime(String date, String time);
    List<Reservation> findByDateAndTimeAndCapacityAndReserved(String date, String time, int capacity, boolean reserved);
    boolean existsByDateAndTimeAndTableNumber(String date, String time, int tableNumber);
}
