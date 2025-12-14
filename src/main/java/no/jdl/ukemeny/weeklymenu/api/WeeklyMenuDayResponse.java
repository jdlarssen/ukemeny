package no.jdl.ukemeny.weeklymenu.api;

public record WeeklyMenuDayResponse(
        int dayOfWeek,
        Long recipeId,
        String recipeName,
        String note
) {}
