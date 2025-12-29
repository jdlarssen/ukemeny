package no.jdl.ukemeny.recipe.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import no.jdl.ukemeny.recipe.RecipeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/recipes")
@Tag(name = "Recipes", description = "Oppskrifter (CRUD-ish) med ingredienser/items")
public class RecipeController {

    private final RecipeService service;

    public RecipeController(RecipeService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Opprett oppskrift",
            description = "Oppretter en ny oppskrift med items. Ingredienser auto-opprettes hvis de ikke finnes."
    )
    public CreateRecipeResponse create(@Valid @RequestBody CreateRecipeRequest request) {
        var id = service.create(request);
        return new CreateRecipeResponse(id);
    }

    @GetMapping
    @Operation(
            summary = "Søk oppskrifter (Top 10)",
            description = "Søker etter oppskrifter på navn (case-insensitive). Returnerer opp til 10 treff."
    )
    public java.util.List<RecipeSummary> search(@RequestParam String name) {
        return service.searchByName(name).stream()
                .map(r -> new RecipeSummary(r.getId(), r.getName()))
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Hent oppskrift",
            description = "Henter en oppskrift med items."
    )
    public RecipeDetailsResponse get(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Oppdater oppskrift (replace items",
            description = "Oppdaterer navn/beskrivelse og overskriver hele items-listen."
    )
    public void update(@PathVariable Long id, @Valid @RequestBody UpdateRecipeRequest request) {
        service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Slett oppskrift",
            description = "Sletter oppskriften. (Merk: oppskriften kan være referert i ukemenyer avhengig av datamodell/DB-regler.)"
    )
    public void delete(@PathVariable Long id){
        service.delete(id);
    }
}
