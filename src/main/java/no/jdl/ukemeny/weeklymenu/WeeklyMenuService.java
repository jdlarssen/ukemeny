package no.jdl.ukemeny.weeklymenu;

import no.jdl.ukemeny.common.NotFoundException;
import no.jdl.ukemeny.recipe.RecipeRepository;
import no.jdl.ukemeny.weeklymenu.api.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WeeklyMenuService {

    private record CatKey(Long categoryId, String categoryName, int sortOrder) {}
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
            entry.setLocked(Boolean.TRUE.equals(d.locked()));
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
                        e.isLocked(),
                        e.getNote()
                ))
                .toList();

        return new WeeklyMenuResponse(menu.getId(), menu.getWeekStartDate(), dinners);
    }
    @Transactional
    public void updateDinner(Long weeklyMenuId, int dayOfWeek, UpdateWeeklyMenuDayRequest req) {
        if (dayOfWeek < 1 || dayOfWeek > 7) {
            throw new IllegalArgumentException("dayOfWeek must be between 1 and 7");
        }

        var menu = weeklyMenuRepository.findByIdWithEntries(weeklyMenuId)
                .orElseThrow(() -> new NotFoundException("Weekly menu not found: " + weeklyMenuId));

        var entry = menu.getEntries().stream()
                .filter(e -> e.getDayOfWeek() == dayOfWeek)
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "Weekly menu entry not found for dayOfWeek " + dayOfWeek + " in weekly menu " + weeklyMenuId
                ));

        var recipe = recipeRepository.findById(req.recipeId())
                .orElseThrow(() -> new NotFoundException("Recipe not found: " + req.recipeId()));

        entry.setRecipe(recipe);
        entry.setLocked(req.locked());
        entry.setNote(req.note());
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
            var entry = new WeeklyMenuEntry(day, recipe, null);
            entry.setLocked(false); // generert = ulåst
            menu.addEntry(entry);

        }

        return weeklyMenuRepository.save(menu).getId();
    }
    @Transactional(readOnly = true)
    public ShoppingListResponse shoppingList(Long weeklyMenuId) {
        var menu = weeklyMenuRepository.findByIdWithEntries(weeklyMenuId)
                .orElseThrow(() -> new NotFoundException("Weekly menu not found: " + weeklyMenuId));

        // Key = ingredientsId + unit (samme ingrediens med ulike unit skal ikke blandes)
        record Key(Long ingredientId, String unit) {
        }

        // Intern accumulator
        class Acc {
            Long ingredientId;
            String ingredientName;
            Long categoryId;
            String categoryName;
            int categorySortOrder;
            String unit;
            BigDecimal total = BigDecimal.ZERO;
            List<ShoppingListItemSource> sources = new ArrayList<>();

            Acc(Long ingredientId, String ingredientName,
                Long categoryId, String categoryName, int categorySortOrder,
                String unit) {
                this.ingredientId = ingredientId;
                this.ingredientName = ingredientName;
                this.categoryId = categoryId;
                this.categoryName = categoryName;
                this.categorySortOrder = categorySortOrder;
                this.unit = unit;
            }

            void add(int dayOfWeek, Long recipeId, String recipeName, BigDecimal amount) {
                total = total.add(amount);
                sources.add(new ShoppingListItemSource(dayOfWeek, recipeId, recipeName, amount, unit));
            }
        }

        Map<Key, Acc> acc = new LinkedHashMap<>();

        for (var dayEntry : menu.getEntries()) {
            var dayOfWeek = dayEntry.getDayOfWeek();
            var recipe = dayEntry.getRecipe();

            var recipeId = recipe.getId();
            var recipeName = recipe.getName();

            for (var item : recipe.getItems()) {
                var ing = item.getIngredient();
                var unit = item.getUnit();
                var cat = ing.getCategory();
                var categoryId = cat.getId();
                var categoryName = cat.getName();
                var categorySortOrder = cat.getSortOrder();
                var key = new Key(ing.getId(), unit);
                var bucket = acc.get(key);
                if (bucket == null) {
                    bucket = new Acc(ing.getId(), ing.getName(), categoryId, categoryName, categorySortOrder, unit);
                    acc.put(key, bucket);
                }

                bucket.add(dayOfWeek, recipeId, recipeName, item.getAmount());
            }
        }

        //Sorter sources inne i hver accumulator
        var grouped = acc.values().stream()
                .collect(Collectors.groupingBy(
                        a -> new CatKey(a.categoryId, a.categoryName, a.categorySortOrder),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        var categories = grouped.entrySet().stream()
                .sorted(Comparator
                        .comparingInt((Map.Entry<CatKey, List<Acc>> e) -> e.getKey().sortOrder())
                        .thenComparing(e -> e.getKey().categoryName(), String.CASE_INSENSITIVE_ORDER)
                )
                .map(e -> {
                    var items = e.getValue().stream()
                            .peek(a -> a.sources.sort(Comparator.comparingInt(ShoppingListItemSource::dayOfWeek)))
                            .sorted(Comparator.comparing(a -> a.ingredientName, String.CASE_INSENSITIVE_ORDER))
                            .map(a -> new ShoppingListItemResponse(
                                    a.ingredientId,
                                    a.ingredientName,
                                    a.total,
                                    a.unit,
                                    List.copyOf(a.sources)
                            ))
                            .toList();

                    return new ShoppingListCategoryResponse(e.getKey().categoryName(), items);
                })
                .toList();
        return new ShoppingListResponse(menu.getId(), menu.getWeekStartDate(), categories);
    }
}
