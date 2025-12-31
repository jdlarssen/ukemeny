package no.jdl.ukemeny.ingredient.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.jdl.ukemeny.ingredient.CategoryRepository;
import no.jdl.ukemeny.ingredient.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@Tag(name = "Categories", description = "Kategorier (sortert etter sortOrder)")
@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryRepository repo;
    private final CategoryService service;

    public CategoryController(CategoryRepository repo, CategoryService service) {
        this.repo = repo;
        this.service = service;
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

    @Operation(
            summary = "Oppdater kategori (PATCH)",
            description = "Oppdaterer name og/eller sortOrder."
    )
    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void patch(@PathVariable Long id, @RequestBody UpdateCategoryRequest req) {
        service.patch(id, req);
    }

}