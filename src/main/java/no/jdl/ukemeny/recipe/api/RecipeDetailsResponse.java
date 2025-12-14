package no.jdl.ukemeny.recipe.api;

import java.util.List;

public record RecipeDetailsResponse (
        Long id,
        String name,
        String description,
        List<RecipeItemResponse> items
){}
