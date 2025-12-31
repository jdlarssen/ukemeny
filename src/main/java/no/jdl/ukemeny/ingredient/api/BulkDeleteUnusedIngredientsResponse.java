package no.jdl.ukemeny.ingredient.api;

import java.util.List;

public record BulkDeleteUnusedIngredientsResponse(
        List<Long> deletedIds,
        List<Long> skippedUsedIds,
        List<Long> skippedNotFoundIds
) {}