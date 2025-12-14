package no.jdl.ukemeny.weeklymenu.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record WeeklyMenuEntryRequest(
        @Min(1) @Max(7) int dayOfWeek,
        @NotNull Long recipeId,
        String note
) {}
