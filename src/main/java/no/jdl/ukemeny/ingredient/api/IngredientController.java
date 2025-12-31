// IngredientController.java
package no.jdl.ukemeny.ingredient.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import no.jdl.ukemeny.ingredient.IngredientService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ingredients")
@Tag(name = "Ingredients", description = "Ingrediens-vedlikehold (liste, kategorisering, sletting)")
public class IngredientController {

    private final IngredientService service;

    public IngredientController(IngredientService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(
            summary = "List ingredienser",
            description = "Lister alle ingredienser med kategori. Sortert etter category.sortOrder og deretter navn."
    )
    public List<IngredientResponse> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean unused
    ) {
        return service.list(query, categoryId, unused);
    }

    public record SetCategoryRequest(@NotNull Long categoryId) {}

    @PatchMapping("/{id}/category")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Sett kategori på ingrediens (single)",
            description = "Oppdaterer categoryId for én ingrediens."
    )
    public void setCategory(@PathVariable Long id, @RequestBody @Valid SetCategoryRequest req) {
        service.setCategory(id, req.categoryId());
    }

    public record BulkSetCategoryItem(
            @NotNull Long ingredientId,
            @NotNull Long categoryId
    ) {}

    public record BulkSetCategoryRequest(
            @NotEmpty List<@Valid BulkSetCategoryItem> updates
    ) {}

    @PatchMapping("/category")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Sett kategori på ingredienser (bulk)",
            description = "Bulk-oppdatering av kategori for flere ingredienser i én request. Feiler med 404 hvis en ingredientId/categoryId ikke finnes."
    )
    public void bulkSetCategory(@RequestBody @Valid BulkSetCategoryRequest req) {
        service.setCategories(req.updates());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Slett ingrediens (kun hvis ubrukt)",
            description = "Sletter ingrediens hvis den ikke brukes av noen oppskrift. Returnerer 409 Conflict hvis den er i bruk."
    )
    public void delete(@PathVariable Long id) {
        service.deleteIfUnused(id);
    }
}
