package no.jdl.ukemeny;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UkemenyApplicationTests {

	@Autowired
	TestRestTemplate http;

	// DTO-er som matcher JSON fra API-et
	record IdResponse(Long id) {}

	record RecipeItemDto(Long id, Long ingredientId, String ingredientName,
						 BigDecimal amount, String unit, String note) {}

	record RecipeDto(Long id, String name, String description, List<RecipeItemDto> items) {}

	record IngredientDto(Long id, String name, Long categoryId, String categoryName) {}

	record CategoryDto(Long id, String name, int sortOrder) {}

	@Test
	void creatingRecipeWithNewIngredient_assignsDefaultCategoryDiverse() {
		var ingredientName = "CI-default-" + UUID.randomUUID().toString().substring(0, 8);

		var recipeId = createRecipe("CI Default category", "test", ingredientName);

		// hent oppskriften for å finne ingredientId (siden det er lettest og mest robust)
		var recipe = http.getForObject("/recipes/" + recipeId, RecipeDto.class);
		assertThat(recipe).isNotNull();
		assertThat(recipe.items()).isNotEmpty();

		var createdIng = recipe.items().getFirst();
		assertThat(createdIng.ingredientName()).isNotBlank();

		// verifiser i /ingredients at kategorien er Diverse
		var ingredients = getIngredients();
		var match = ingredients.stream()
				.filter(i -> i.id().equals(createdIng.ingredientId()))
				.findFirst();

		assertThat(match).isPresent();
		assertThat(match.get().categoryName()).isEqualTo("Diverse");
	}

	@Test
	void ingredientsList_isOrderedByCategorySortOrder_thenByName() {
		// Finn ID-er for to kategorier med ulik sortOrder
		var categories = getCategories();
		var kjott = categories.stream().filter(c -> c.name().equalsIgnoreCase("Kjøtt")).findFirst().orElseThrow();
		var diverse = categories.stream().filter(c -> c.name().equalsIgnoreCase("Diverse")).findFirst().orElseThrow();

		// Lag to ingredienser via recipe-API (så vi tester hele flyten)
		var ingAName = "CI-order-a-" + UUID.randomUUID().toString().substring(0, 8);
		var ingBName = "CI-order-b-" + UUID.randomUUID().toString().substring(0, 8);

		var recipeAId = createRecipe("CI order A", "test", ingAName);
		var recipeBId = createRecipe("CI order B", "test", ingBName);

		var recipeA = http.getForObject("/recipes/" + recipeAId, RecipeDto.class);
		var recipeB = http.getForObject("/recipes/" + recipeBId, RecipeDto.class);

		var ingAId = Objects.requireNonNull(recipeA).items().getFirst().ingredientId();
		var ingBId = Objects.requireNonNull(recipeB).items().getFirst().ingredientId();

		// Sett kategori: A -> Kjøtt (lavere sortOrder), B -> Diverse (høyere sortOrder)
		patchIngredientCategory(ingAId, kjott.id());
		patchIngredientCategory(ingBId, diverse.id());

		var ingredients = getIngredients();

		int idxA = indexOfIngredient(ingredients, ingAId);
		int idxB = indexOfIngredient(ingredients, ingBId);

		assertThat(idxA).isGreaterThanOrEqualTo(0);
		assertThat(idxB).isGreaterThanOrEqualTo(0);

		// Kjøtt (sortOrder 5) skal komme før Diverse (sortOrder 16)
		assertThat(idxA).isLessThan(idxB);
	}

	@Test
	void deletingIngredientUsedByRecipe_returns409Conflict() {
		var ingredientName = "CI-delete-" + UUID.randomUUID().toString().substring(0, 8);
		var recipeId = createRecipe("CI delete test", "test", ingredientName);

		var recipe = http.getForObject("/recipes/" + recipeId, RecipeDto.class);
		assertThat(recipe).isNotNull();

		var ingredientId = recipe.items().getFirst().ingredientId();

		var resp = http.exchange("/ingredients/" + ingredientId, HttpMethod.DELETE, null, String.class);

		assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
	}

	// -------- helpers --------

	private Long createRecipe(String name, String description, String ingredientName) {
		var body = """
            {
              "name": "%s",
              "description": "%s",
              "items": [
                { "ingredientName": "%s", "amount": 1, "unit": "stk", "note": null }
              ]
            }
            """.formatted(name, description, ingredientName);

		var headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		var resp = http.postForEntity("/recipes", new HttpEntity<>(body, headers), IdResponse.class);
		assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(Objects.requireNonNull(resp.getBody()).id()).isNotNull();

		return resp.getBody().id();
	}

	private List<IngredientDto> getIngredients() {
		var resp = http.exchange("/ingredients", HttpMethod.GET, null,
				new org.springframework.core.ParameterizedTypeReference<List<IngredientDto>>() {});
		assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
		return Objects.requireNonNull(resp.getBody());
	}

	private List<CategoryDto> getCategories() {
		var resp = http.exchange("/categories", HttpMethod.GET, null,
				new org.springframework.core.ParameterizedTypeReference<List<CategoryDto>>() {});
		assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
		return Objects.requireNonNull(resp.getBody());
	}

	private void patchIngredientCategory(Long ingredientId, Long categoryId) {
		var body = "{ \"categoryId\": " + categoryId + " }";

		var headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		var resp = http.exchange(
				"/ingredients/" + ingredientId + "/category",
				HttpMethod.PATCH,
				new HttpEntity<>(body, headers),
				Void.class
		);

		assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
	}

	private int indexOfIngredient(List<IngredientDto> list, Long id) {
		for (int i = 0; i < list.size(); i++) {
			if (Objects.equals(list.get(i).id(), id)) return i;
		}
		return -1;
	}
}