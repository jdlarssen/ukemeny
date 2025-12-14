package no.jdl.ukemeny.weeklymenu;

import no.jdl.ukemeny.common.NotFoundException;
import no.jdl.ukemeny.recipe.RecipeRepository;
import no.jdl.ukemeny.weeklymenu.api.CreateWeeklyMenuRequest;
import no.jdl.ukemeny.weeklymenu.api.WeeklyMenuDayResponse;
import no.jdl.ukemeny.weeklymenu.api.WeeklyMenuResponse;
import no.jdl.ukemeny.weeklymenu.api.ShoppingListItemResponse;
import no.jdl.ukemeny.weeklymenu.api.ShoppingListResponse;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class WeeklyMenuService {

    private final WeeklyMenuRepository weeklyMenuRepository;
    private final RecipeRepository recipeRepository;

    public WeeklyMenuService(WeeklyMenuRepository weeklyMenuRepository, RecipeRepository recipeRepository) {
        this.weeklyMenuRepository = weeklyMenuRepository;
        this.recipeRepository = recipeRepository;
    }

    @Transactional
    public Long create(CreateWeeklyMenuRequest req) {
        if (req.weekStartDate().getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new IllegalArgumentException("weekStartDate must be a Monday");
        }

        var menu = new WeeklyMenu(req.weekStartDate());

        // (MVP) lar vi DB sin UNIQUE(weekly_menu_id, day_of_week) håndheve at du ikke kan ha ta middager samme dag.
        for (var d : req.dinners()) {
            var recipe = recipeRepository.findById(d.recipeId())
                    .orElseThrow(() -> new NotFoundException("Recipe not found: " + d.recipeId()));

            var entry = new WeeklyMenuEntry(d.dayOfWeek(), recipe, d.note());
            menu.addEntry(entry);
        }

        return weeklyMenuRepository.save(menu).getId();
    }

    @Transactional(readOnly = true)
    public WeeklyMenuResponse get(Long id) {
        var menu = weeklyMenuRepository.findByIdWithEntries(id)
                .orElseThrow(() -> new NotFoundException("Weekly menu not found: " + id));

        var dinners = menu.getEntries().stream()
                .sorted(Comparator.comparingInt(WeeklyMenuEntry::getDayOfWeek))
                .map(e -> new WeeklyMenuDayResponse(
                        e.getDayOfWeek(),
                        e.getRecipe().getId(),
                        e.getRecipe().getName(),
                        e.getNote()
                ))
                .toList();

        return new WeeklyMenuResponse(menu.getId(), menu.getWeekStartDate(), dinners);
    }
    @Transactional
    public Long generate(java.time.LocalDate weekStartDate) {
        if (weekStartDate.getDayOfWeek() != DayOfWeek.MONDAY){
            throw new IllegalArgumentException("weekStartDate must be a Monday");
        }

        var allIds = recipeRepository.findAllIds();
        if (allIds.isEmpty()){
            throw new IllegalArgumentException("No recipes exist. Create at least 1 recipe first.");
        }

        var previousOpt = weeklyMenuRepository
                .findPrevious(weekStartDate, PageRequest.of(0, 1))
                .stream()
                .findFirst();

        java.util.Set<Long> previousRecipeIds = java.util.Collections.emptySet();
        if (previousOpt.isPresent()){
            var prev = weeklyMenuRepository.findByIdWithEntries(previousOpt.get().getId())
                    .orElse(previousOpt.get());

            previousRecipeIds = prev.getEntries().stream()
                    .map(e -> e.getRecipe().getId())
                    .collect(Collectors.toSet());
        }

        final var prevIds = previousRecipeIds;

        var rng = new Random();

        // Del opp i "nye" og "brukte sist"
        var fresh = allIds.stream()
                .filter (id -> !prevIds.contains(id))
                .collect(Collectors.toCollection(ArrayList::new));

        var used = allIds.stream()
                .filter(prevIds::contains)
                .collect(Collectors.toCollection(ArrayList::new));

        Collections.shuffle(fresh, rng);
        Collections.shuffle(used, rng);

        var selected = new ArrayList<Long>();

        if (allIds.size() >= 7) {
            // Ingen repeats: fyll først fra fresh, så fra used
            selected.addAll(fresh);
            if (selected.size() < 7) {
                selected.addAll(used);
            }
            selected = new ArrayList<>(selected.subList(0, 7));
        } else {
            // Repeats tillatt: fyll først fra fresh+used, deretter random repeats
            selected.addAll(fresh);
            selected.addAll(used);

            while (selected.size() < 7 ) {
                selected.add(allIds.get(rng.nextInt(allIds.size())));
            }
            selected = new ArrayList<>(selected.subList(0, 7));
        }
        // Hent oppskrifter i bulk
        var recipes = recipeRepository.findAllById(selected);
        var byId = new HashMap<Long, no.jdl.ukemeny.recipe.Recipe>();
        for (var r : recipes) byId.put(r.getId(), r);

        var menu = new WeeklyMenu(weekStartDate);

        for (int day = 1; day <= 7; day++) {
            var recipeId = selected.get(day - 1);
            var recipe = byId.get(recipeId);
            if (recipe == null) {
                // Bør ikke skje, men gir en ryddig feil om DB endrer seg midt i
                throw new IllegalArgumentException("Recipe not found during generation: " + recipeId);
            }
            menu.addEntry(new WeeklyMenuEntry(day, recipe, null));
        }

        return weeklyMenuRepository.save(menu).getId();
    }
    @Transactional(readOnly = true)
    public ShoppingListResponse shoppingList(Long weeklyMenuId) {
        var menu = weeklyMenuRepository.findByIdWithEntries(weeklyMenuId)
                .orElseThrow(() -> new NotFoundException("Weekly menu not found: " + weeklyMenuId));

        var recipeIds = menu.getEntries().stream()
                .map(e -> e.getRecipe().getId())
                .collect(Collectors.toSet());

        var recipes = recipeRepository.findAllByIdWithItems(recipeIds);

        var recipeById = recipes.stream()
                .collect(Collectors.toMap(r -> r.getId(), r -> r));

        // Key = ingredientId + unit (samme ingrediens med ulike unit skal ikke blandes)
        record Key(Long ingredientId, String unit) {}
        Map<Key, ShoppingListItemResponse> acc = new LinkedHashMap<>();

        for (var entry : menu.getEntries()) {
            var recipe = recipeById.get(entry.getRecipe().getId());
            if (recipe == null) continue; // burde ikke skje

            for (var item : recipe.getItems()) {
                var ing = item.getIngredient();
                var key = new Key(ing.getId(), item.getUnit());

                var existing = acc.get(key);
                if (existing == null) {
                    acc.put(key, new ShoppingListItemResponse(
                            ing.getId(),
                            ing.getName(),
                            item.getAmount(),
                            item.getUnit()
                    ));
                } else {
                    acc.put(key, new ShoppingListItemResponse(
                            existing.ingredientId(),
                            existing.ingredientName(),
                            existing.amount().add(item.getAmount()),
                            existing.unit()
                    ));
                }
            }
        }

        var items = acc.values().stream()
                .sorted((a, b) -> a.ingredientName().compareToIgnoreCase(b.ingredientName()))
                .toList();

        return new ShoppingListResponse(menu.getId(), menu.getWeekStartDate(), items);
    }
}
