package com.SampleRestBot.SampleRestBot.mySource;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class GetNearThreeDays {

    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static String getToday() {
        return LocalDate.now().format(DEFAULT_FORMATTER);
    }

    public static String getTomorrow() {
        return LocalDate.now().plusDays(1).format(DEFAULT_FORMATTER);
    }

    public static String getNextTomorrow() {
        return LocalDate.now().plusDays(2).format(DEFAULT_FORMATTER);
    }

}