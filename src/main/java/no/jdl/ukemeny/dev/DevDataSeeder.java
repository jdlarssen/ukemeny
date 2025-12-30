package no.jdl.ukemeny.dev;

import no.jdl.ukemeny.ingredient.CategoryRepository;
import no.jdl.ukemeny.recipe.RecipeService;
import no.jdl.ukemeny.recipe.api.CreateRecipeItemRequest;
import no.jdl.ukemeny.recipe.api.CreateRecipeRequest;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@Profile("dev")
public class DevDataSeeder implements ApplicationRunner {

    private final RecipeService recipeService;
    private final CategoryRepository categoryRepository;

    public DevDataSeeder(RecipeService recipeService, CategoryRepository categoryRepository) {
        this.recipeService = recipeService;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Forutsetter at categories allerede er seeded via Flyway (V7__seed_categories.sql)
        // Vi trenger ikke kategorier for å lage oppskrifter, men repo’en er her om du vil utvide senere.
        if (categoryRepository.count() == 0) {
            return;
        }

        // 10 ingredienser totalt på tvers av disse oppskriftene (med overlapp):
        // Kjøttdeig, Tacokrydder, Tortilla, Ost, Salat, Tomat, Løk, Hvitløk, Pasta, Creme fraiche

        seed("Dev Taco", "Rask taco",
                item("Kjøttdeig", "400", "g"),
                item("Tacokrydder", "1", "pose"),
                item("Tortilla", "8", "stk"),
                item("Ost", "150", "g"),
                item("Salat", "1", "stk"),
                item("Tomat", "2", "stk"),
                item("Løk", "1", "stk")
        );

        seed("Dev Spaghetti bolognese", "Klassiker",
                item("Kjøttdeig", "400", "g"),
                item("Pasta", "400", "g"),
                item("Tomat", "2", "stk"),
                item("Løk", "1", "stk"),
                item("Hvitløk", "2", "fedd")
        );

        seed("Dev Pasta carbonara-ish", "Enkel variant",
                item("Pasta", "400", "g"),
                item("Ost", "100", "g"),
                item("Hvitløk", "1", "fedd")
        );

        seed("Dev Kjøttdeig og pasta", "Hverdags",
                item("Kjøttdeig", "300", "g"),
                item("Pasta", "300", "g"),
                item("Løk", "1", "stk"),
                item("Hvitløk", "1", "fedd")
        );

        seed("Dev Taco bowl", "Taco uten tortilla",
                item("Kjøttdeig", "400", "g"),
                item("Tacokrydder", "1", "pose"),
                item("Salat", "1", "stk"),
                item("Tomat", "2", "stk"),
                item("Løk", "1", "stk"),
                item("Creme fraiche", "200", "g")
        );

        seed("Dev Quesadilla", "Ost + tortilla",
                item("Tortilla", "6", "stk"),
                item("Ost", "200", "g"),
                item("Løk", "1", "stk"),
                item("Tacokrydder", "1", "pose")
        );

        seed("Dev Pastasalat", "Kald",
                item("Pasta", "300", "g"),
                item("Salat", "1", "stk"),
                item("Tomat", "2", "stk"),
                item("Løk", "1", "stk"),
                item("Ost", "150", "g")
        );

        seed("Dev Taco leftovers", "Rydd-i-kjøleskapet",
                item("Kjøttdeig", "200", "g"),
                item("Tacokrydder", "1", "pose"),
                item("Tortilla", "4", "stk"),
                item("Ost", "100", "g")
        );
    }

    private void seed(String name, String description, CreateRecipeItemRequest... items) {
        // Idempotent: ikke opprett samme dev-oppskrift flere ganger
        if (!recipeService.searchByName(name).isEmpty()) {
            return;
        }

        var req = new CreateRecipeRequest(name, description, List.of(items));
        recipeService.create(req);
    }

    private CreateRecipeItemRequest item(String ingredientName, String amount, String unit) {
        return new CreateRecipeItemRequest(
                ingredientName,
                new BigDecimal(amount),
                unit,
                null
        );
    }
}