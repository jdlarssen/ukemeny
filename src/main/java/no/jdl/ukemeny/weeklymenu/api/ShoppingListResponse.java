package no.jdl.ukemeny.weeklymenu.api;

import java.time.LocalDate;
import java.util.List;

public record ShoppingListResponse(
        Long weeklyMenuId,
        LocalDate weekStartDate,
        List<ShoppingListCategoryResponse> categories
) {}