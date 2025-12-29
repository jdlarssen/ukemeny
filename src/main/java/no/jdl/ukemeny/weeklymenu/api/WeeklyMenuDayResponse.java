package no.jdl.ukemeny.weeklymenu.api;

public record WeeklyMenuDayResponse(
        int dayOfWeek,
        Long recipeId,
        String recipeName,
        boolean locked,
        String note
) {}
