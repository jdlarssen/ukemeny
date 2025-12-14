package no.jdl.ukemeny.recipe;

import no.jdl.ukemeny.ingredient.IngredientService;
import no.jdl.ukemeny.recipe.api.CreateRecipeRequest;
import no.jdl.ukemeny.recipe.api.RecipeDetailsResponse;
import no.jdl.ukemeny.recipe.api.RecipeItemResponse;
import no.jdl.ukemeny.common.NotFoundException;
import no.jdl.ukemeny.recipe.api.UpdateRecipeRequest;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final IngredientService ingredientService;

    public RecipeService(RecipeRepository recipeRepository, IngredientService ingredientService) {
        this.recipeRepository = recipeRepository;
        this.ingredientService = ingredientService;
    }
    @Transactional
    public void update(Long id, UpdateRecipeRequest req){
        var recipe = recipeRepository.findByIdWithItems(id)
                .orElseThrow(() -> new NotFoundException("Recipe not found: " + id));

        // Oppdatert "simple" felter
        recipe.setName(req.name());
        recipe.setDescription(req.description());

        // Erstatt items
        recipe.clearItems();

        for (var itemReq : req.items()) {
            var ingredient = ingredientService.getOrCreate(itemReq.ingredientName());
            var item = new RecipeItem(
                    recipe,
                    ingredient,
                    itemReq.amount(),
                    itemReq.unit(),
                    itemReq.note()
            );
            recipe.addItem(item);
        }

        recipeRepository.save(recipe);
    }
    @Transactional
    public Long create(CreateRecipeRequest req) {
        var recipe = new Recipe(req.name(), req.description());

        for (var itemReq : req.items()){
            var ingredient = ingredientService.getOrCreate(itemReq.ingredientName());
            var item = new RecipeItem(
                    recipe,
                    ingredient,
                    itemReq.amount(),
                    itemReq.unit(),
                    itemReq.note()
            );
            recipe.addItem(item);
        }

        return recipeRepository.save(recipe).getId();
    }

    @Transactional
    public void delete(Long id){
        if (!recipeRepository.existsById(id)) {
            throw new NotFoundException(("Recipe not found: " + id));
        }
        recipeRepository.deleteById(id);
    }
    public List<Recipe> searchByName(String name) {
        return recipeRepository.findTop10ByNameContainingIgnoreCaseOrderByIdDesc(name.trim());
    }

    public RecipeDetailsResponse getById(Long id) {
        var recipe = recipeRepository.findByIdWithItems(id)
                .orElseThrow(() -> new NotFoundException("Recipe not found: " + id));

        var items = recipe.getItems().stream()
                .map(i -> new RecipeItemResponse(
                        i.getId(),
                        i.getIngredient().getId(),
                        i.getIngredient().getName(),
                        i.getAmount(),
                        i.getUnit(),
                        i.getNote()
                ))
                .collect(Collectors.toList());

        return new RecipeDetailsResponse(
                recipe.getId(),
                recipe.getName(),
                recipe.getDescription(),
                items
        );
    }
}
