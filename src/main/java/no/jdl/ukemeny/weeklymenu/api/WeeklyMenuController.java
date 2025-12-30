// WeeklyMenuController.java
package no.jdl.ukemeny.weeklymenu.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import no.jdl.ukemeny.weeklymenu.WeeklyMenuService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/weekly-menus")
@Tag(name = "Weekly menus", description = "Ukemenyer + handleliste")
public class WeeklyMenuController {

    private final WeeklyMenuService service;

    public WeeklyMenuController(WeeklyMenuService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Opprett ukemeny (manuelt)",
            description = "Oppretter ukemeny for en uke. weekStartDate må være mandag. dinners inneholder dayOfWeek (1-7), recipeId, locked og ev. note."
    )
    public CreateWeeklyMenuResponse create(@Valid @RequestBody CreateWeeklyMenuRequest request) {
        return new CreateWeeklyMenuResponse(service.create(request));
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Generer ukemeny automatisk",
            description = "Genererer en ukemeny for en uke (weekStartDate må være mandag). Prøver å variere fra forrige uke når mulig."
    )
    public CreateWeeklyMenuResponse generate(@Valid @RequestBody GenerateWeeklyMenuRequest request) {
        return new CreateWeeklyMenuResponse(service.generate(request.weekStartDate()));
    }

    @PostMapping("/{id}/regenerate")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Regenerer ukemeny (kun ulåste dager)",
            description = "Bytter kun ut middager der locked=false. Låste dager beholdes."
    )
    public WeeklyMenuResponse regenerate(@PathVariable Long id) {
        return service.regenerateUnlocked(id);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Hent ukemeny",
            description = "Henter ukemeny med alle middager (dinners), sortert på dayOfWeek."
    )
    public WeeklyMenuResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PatchMapping("/{id}/dinners/{dayOfWeek}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Oppdater middag (bytt recipe + locked + note)",
            description = "Oppdaterer én dag i ukemenyen. dayOfWeek er 1-7. Setter recipeId/locked/note."
    )
    public void updateDinner(
            @PathVariable Long id,
            @PathVariable int dayOfWeek,
            @Valid @RequestBody UpdateWeeklyMenuDayRequest request
    ) {
        service.updateDinner(id, dayOfWeek, request);
    }

    @GetMapping("/{id}/shopping-list")
    @Operation(
            summary = "Hent handleliste for ukemeny",
            description = "Returnerer handleliste aggregert på ingrediens + unit, gruppert og sortert etter kategori (category.sortOrder). Inkluderer 'sources' per linje (hvilke dager/oppskrifter som bidro)."
    )
    public ShoppingListResponse shoppingList(@PathVariable Long id) {
        return service.shoppingList(id);
    }
}
