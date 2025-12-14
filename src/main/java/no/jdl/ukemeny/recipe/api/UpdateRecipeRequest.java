package no.jdl.ukemeny.recipe.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateRecipeRequest (
    @NotBlank String name,
    String description,
    @NotEmpty @Valid List<CreateRecipeItemRequest> items
) {}