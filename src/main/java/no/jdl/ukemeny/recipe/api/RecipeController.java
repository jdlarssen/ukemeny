package no.jdl.ukemeny.recipe.api;

import no.jdl.ukemeny.recipe.RecipeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/recipes")
public class RecipeController {

    private final RecipeService service;

    public RecipeController(RecipeService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateRecipeResponse create(@Valid @RequestBody CreateRecipeRequest request) {
        var id = service.create(request);
        return new CreateRecipeResponse(id);
    }

    @GetMapping
    public java.util.List<RecipeSummary> search(@RequestParam String name) {
        return service.searchByName(name).stream()
                .map(r -> new RecipeSummary(r.getId(), r.getName()))
                .toList();
    }

    @GetMapping("/{id}")
    public RecipeDetailsResponse get(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable Long id, @Valid @RequestBody UpdateRecipeRequest request) {
        service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id){
        service.delete(id);
    }
}
