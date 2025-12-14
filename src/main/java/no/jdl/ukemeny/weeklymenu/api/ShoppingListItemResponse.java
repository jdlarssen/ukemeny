package no.jdl.ukemeny.weeklymenu.api;

import java.math.BigDecimal;

public record ShoppingListItemResponse(
        Long ingredientId,
        String ingredientName,
        BigDecimal amount,
        String unit
) {}
