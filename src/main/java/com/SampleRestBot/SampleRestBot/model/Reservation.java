package com.SampleRestBot.SampleRestBot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String date;       // Дата бронирования
    private String time;       // Время бронирования
    private int tableNumber;   // Номер столика
    private int capacity;      // Вместимость столика
    private String waiterName;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean reserved;

    @Override
    public String toString() {
        return "Reservation{" +
                "id=" + id +
                ", date='" + date + '\'' +
                ", time='" + time + '\'' +
                ", tableNumber=" + tableNumber +
                ", capacity=" + capacity +
                ", reserved=" + reserved +
                ", waiterName='" + waiterName + '\'' +
                '}';
    }
}

