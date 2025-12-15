package no.jdl.ukemeny.ingredient;

import no.jdl.ukemeny.common.NotFoundException;
import no.jdl.ukemeny.ingredient.api.IngredientResponse;
import no.jdl.ukemeny.ingredient.api.IngredientController.BulkSetCategoryItem;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.CONFLICT;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class IngredientService {

    private final CategoryRepository categoryRepo;

    private final IngredientRepository repo;

    public IngredientService(IngredientRepository repo, CategoryRepository categoryRepo) {

        this.repo = repo;
        this.categoryRepo = categoryRepo;
    }

    @Transactional
    public Ingredient getOrCreate(String name) {
        var normalized = normalizeName(name);

        var defaultCategory = categoryRepo.findByNameIgnoreCase("Diverse")
                .orElseThrow(() -> new IllegalStateException("Default category 'Diverse' missing"));

        return repo.findByNameIgnoreCase(normalized)
                .orElseGet(() -> {
                    try {
                        return repo.save(new Ingredient(normalized, defaultCategory));
                    } catch (DataIntegrityViolationException e) {
                        return repo.findByNameIgnoreCase(normalized).orElseThrow(() -> e);
                    }
                });
    }

    @Transactional
    public void setCategory(Long ingredientId, Long categoryId) {
        var ingredient = repo.findById(ingredientId)
                .orElseThrow(() -> new NotFoundException("Ingredient not found: " + ingredientId));

        var category = categoryRepo.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found: " + categoryId));

        ingredient.setCategory(category);
        // ingen repo.save nødvendig hvis entity er managed i transaksjonen.
    }

    @Transactional(readOnly = true)
    public List<no.jdl.ukemeny.ingredient.api.IngredientResponse> list() {
        return repo.findAllWithCategoryOrdered().stream()
                .map(i -> new IngredientResponse(
                        i.getId(),
                        i.getName(),
                        i.getCategory().getId(),
                        i.getCategory().getName()
                ))
                .toList();
    }

    private String normalizeName(String input) {
        if (input == null) return null;
        var trimmed = input.trim();
        if (trimmed.isEmpty()) return trimmed;

        // MVP: gjør kun "pent" visningsnavn, men bevarer ordet.
        return trimmed.substring(0,1).toUpperCase() + trimmed.substring(1).toLowerCase();
    }

    @Transactional
    public void setCategories(List<BulkSetCategoryItem> updates) {
        var ingredientIds = updates.stream()
                .map(BulkSetCategoryItem::ingredientId)
                .distinct()
                .toList();
        var categoryIds = updates.stream()
                .map(BulkSetCategoryItem::categoryId)
                .distinct()
                .toList();
        var ingredients = repo.findAllById(ingredientIds);
        var categories = categoryRepo.findAllById(categoryIds);

        Map<Long, Ingredient> ingredientById = ingredients.stream()
                .collect(Collectors.toMap(Ingredient::getId, Function.identity()));
        Map<Long, Category> categoryById = categories.stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));
        for (var id : ingredientIds) {
            if (!ingredientById.containsKey(id)) {
                throw new NotFoundException("Ingredient not found: " + id);
            }
        }
        for (var id : categoryIds) {
            if (!categoryById.containsKey(id)) {
                throw new NotFoundException("Category not found: " + id);
            }
        }
        for (var u : updates) {
            var ing = ingredientById.get(u.ingredientId());
            var cat = categoryById.get(u.categoryId());
            ing.setCategory(cat);
        }
    }
    @Transactional
    public void deleteIfUnused(Long id) {
        var ingredient = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Ingredient not found: " + id));
        if (repo.isUsedInAnyRecipe(id)) {
            throw new ResponseStatusException(CONFLICT,
                    "Ingredient " + id + " is used by a recipe and cannot be deleted");
        }
        repo.delete(ingredient);
    }
}
