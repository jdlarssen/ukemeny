package no.jdl.ukemeny.ingredient.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import no.jdl.ukemeny.ingredient.CategoryRepository;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Categories", description = "Kategorier (sortert etter sortOrder)")
@RestController
@RequestMapping("/categories")
public class CategoryController {
    private final CategoryRepository repo;

    public CategoryController(CategoryRepository repo) {
        this.repo = repo;
    }

    @Operation(
            summary = "List kategorier",
            description = "Lister alle kategorier sortert etter sortOrder (og navn ved lik sortOrder)."
    )
    @GetMapping
    public List<CategoryResponse> list() {
        return repo.findAllByOrderBySortOrderAscNameAsc().stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getSortOrder()))
                .toList();
    }
}