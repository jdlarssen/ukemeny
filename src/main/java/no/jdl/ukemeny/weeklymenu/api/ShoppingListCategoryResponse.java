package no.jdl.ukemeny.weeklymenu.api;

import java.util.List;

public record ShoppingListCategoryResponse(
        String category,
        List<ShoppingListItemResponse> items
) {}