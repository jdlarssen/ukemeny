package no.jdl.ukemeny.weeklymenu.api;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record GenerateWeeklyMenuRequest(@NotNull LocalDate weekStartDate) {}