package com.SampleRestBot.SampleRestBot.model;

public class UserSession {
    private long chatId;
    private int capacity; // Количество человек для бронирования
    private String selectedDate; // Дата бронирования
    private String selectedTime; // Время бронирования

    // Конструктор
    public UserSession(long chatId) {
        this.chatId = chatId;
    }

    // Геттеры и сеттеры
    public long getChatId() {
        return chatId;
    }

    public void setChatId(long chatId) {
        this.chatId = chatId;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getSelectedDate() {
        return selectedDate;
    }

    public void setSelectedDate(String selectedDate) {
        this.selectedDate = selectedDate;
    }

    public String getSelectedTime() {
        return selectedTime;
    }

    public void setSelectedTime(String selectedTime) {
        this.selectedTime = selectedTime;
    }
}

