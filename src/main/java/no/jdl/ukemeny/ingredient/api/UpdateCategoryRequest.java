package no.jdl.ukemeny.ingredient.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateCategoryRequest(
        String name,
        @Min(0) @Max(10_000) Integer sortOrder
) {}
