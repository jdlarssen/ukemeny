package no.jdl.ukemeny.ingredient.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BulkDeleteUnusedIngredientsRequest(
        @NotEmpty List<@NotNull Long> ingredientIds
) {}
