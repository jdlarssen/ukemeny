package no.jdl.ukemeny.weeklymenu.api;

import jakarta.validation.Valid;
import no.jdl.ukemeny.weeklymenu.WeeklyMenuService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/weekly-menus")
public class WeeklyMenuController {

    private final WeeklyMenuService service;

    public WeeklyMenuController(WeeklyMenuService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateWeeklyMenuResponse create(@Valid @RequestBody CreateWeeklyMenuRequest request) {
        return new CreateWeeklyMenuResponse(service.create(request));
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateWeeklyMenuResponse generate(@Valid @RequestBody GenerateWeeklyMenuRequest request) {
        return new CreateWeeklyMenuResponse(service.generate(request.weekStartDate()));
    }

    @GetMapping("/{id}")
    public WeeklyMenuResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/{id}/shopping-list")
    public ShoppingListResponse shoppingList(@PathVariable Long id) {
        return service.shoppingList(id);
    }
}