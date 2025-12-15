package no.jdl.ukemeny.weeklymenu.api;

import no.jdl.ukemeny.weeklymenu.api.ShoppingListItemSource;

import java.math.BigDecimal;
import java.util.List;

public record ShoppingListItemResponse(
        Long ingredientId,
        String ingredientName,
        BigDecimal amount,
        String unit,
        List<ShoppingListItemSource> sources
) {}
