package no.jdl.ukemeny.weeklymenu.api;

import java.time.LocalDate;
import java.util.List;

public record WeeklyMenuResponse (
        Long id,
        LocalDate weekStartDate,
        List<WeeklyMenuDayResponse> dinners
) {}
