package no.jdl.ukemeny.ingredient.api;

public record IngredientResponse(
    Long id,
    String name,
    Long categoryId,
    String categoryName
) {}