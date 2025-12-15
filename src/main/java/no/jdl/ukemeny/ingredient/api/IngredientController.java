package no.jdl.ukemeny.ingredient.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import no.jdl.ukemeny.ingredient.IngredientService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ingredients")
public class IngredientController {

    private final IngredientService service;

    public IngredientController(IngredientService service) {
        this.service = service;
    }

    @GetMapping
    public List<IngredientResponse> list() {
        return service.list();
    }

    public record SetCategoryRequest(@NotNull Long categoryId) {}

    @PatchMapping("/{id}/category")
    @ResponseStatus(HttpStatus.NO_CONTENT)
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
    public void bulkSetCategory(@RequestBody @Valid BulkSetCategoryRequest req) {
        service.setCategories(req.updates());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.deleteIfUnused(id);
    }
}