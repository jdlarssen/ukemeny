package no.jdl.ukemeny.ingredient.api;

import java.util.List;

public record BulkDeleteIngredientsResponse(
        List<Long> deletedIds,
        List<Long> skippedUsedIds
) {}