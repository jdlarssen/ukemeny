package no.jdl.ukemeny.weeklymenu.api;

import jakarta.validation.constraints.NotNull;

public record UpdateWeeklyMenuDayRequest(
        @NotNull Long recipeId,
        @NotNull Boolean locked,
        String note
) {}