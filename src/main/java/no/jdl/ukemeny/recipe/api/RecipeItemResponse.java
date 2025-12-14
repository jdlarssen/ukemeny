package no.jdl.ukemeny.recipe.api;

import java.math.BigDecimal;

public record RecipeItemResponse (
        Long id,
        Long ingredientId,
        String ingredientName,
        BigDecimal amount,
        String unit,
        String note
){}
