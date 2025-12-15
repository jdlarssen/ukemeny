package no.jdl.ukemeny.weeklymenu.api;

import java.math.BigDecimal;

public record ShoppingListItemSource(
        int dayOfWeek,
        Long recipeId,
        String recipeName,
        BigDecimal amount,
        String unit
) {}