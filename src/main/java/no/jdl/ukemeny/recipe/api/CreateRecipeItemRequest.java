package no.jdl.ukemeny.recipe.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateRecipeItemRequest (
        @NotBlank String ingredientName,
        @NotNull BigDecimal amount,
        @NotBlank String unit,
        String note
){}
