package no.jdl.ukemeny;

import no.jdl.ukemeny.ingredient.api.IngredientResponse;
import no.jdl.ukemeny.recipe.api.*;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UkemenyApplicationTests {

	@Autowired
	TestRestTemplate http;

	@Test
	void createRecipe_autoCreatesIngredient_withDefaultCategoryDiverse() {
		var suffix = UUID.randomUUID().toString().substring(0, 8);
		var ingredientInput = "TEST-INGREDIENS-" + suffix; // service normaliserer til "Test-ingrediens-xxxx"
		var recipeName = "Test recipe " + suffix;

		var req = new CreateRecipeRequest(
				recipeName,
				"Sjekk auto-opprett ingrediens + default kategori",
				List.of(new CreateRecipeItemRequest(
						ingredientInput,
						new BigDecimal("1.0"),
						"stk",
						null
				))
		);

		var create = http.postForEntity("/recipes", req, CreateRecipeResponse.class);
		assertThat(create.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(create.getBody()).isNotNull();
		assertThat(create.getBody().id()).isNotNull();

		// Verifiser via GET /ingredients at ingrediensen finnes og har categoryName = "Diverse"
		var ingredientsResp = http.getForEntity("/ingredients", IngredientResponse[].class);
		assertThat(ingredientsResp.getStatusCode()).isEqualTo(HttpStatus.OK);

		var ingredients = List.of(ingredientsResp.getBody() == null ? new IngredientResponse[0] : ingredientsResp.getBody());

		// NB: normalisering i IngredientService: første bokstav stor + resten små
		var expectedName = toNormalizedName(ingredientInput);

		var createdIng = ingredients.stream()
				.filter(i -> i.name().equals(expectedName))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Fant ikke ingrediens i /ingredients: " + expectedName));

		assertThat(createdIng.categoryName()).isEqualTo("Diverse");
	}

	@Test
	void updateRecipe_replacesItemsList() {
		var suffix = UUID.randomUUID().toString().substring(0, 8);
		var recipeName = "Update test " + suffix;

		// create med 2 items
		var createReq = new CreateRecipeRequest(
				recipeName,
				"Oppretter med to items",
				List.of(
						new CreateRecipeItemRequest("Kjøttdeig-" + suffix, new BigDecimal("400"), "g", null),
						new CreateRecipeItemRequest("Tacokrydder-" + suffix, new BigDecimal("1"), "pose", null)
				)
		);

		var created = http.postForEntity("/recipes", createReq, CreateRecipeResponse.class);
		assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

		var id = created.getBody().id();

		// update -> 1 item (overskriv)
		var updateReq = new UpdateRecipeRequest(
				recipeName + " (oppdatert)",
				"Nå bare ett item",
				List.of(new CreateRecipeItemRequest("Kylling-" + suffix, new BigDecimal("500"), "g", null))
		);

		var updateResp = http.exchange("/recipes/" + id, HttpMethod.PUT, new HttpEntity<>(updateReq), Void.class);
		assertThat(updateResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		// fetch -> skal ha 1 item og riktig navn
		var details = http.getForEntity("/recipes/" + id, RecipeDetailsResponse.class);
		assertThat(details.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(details.getBody()).isNotNull();

		assertThat(details.getBody().name()).isEqualTo(recipeName + " (oppdatert)");
		assertThat(details.getBody().items()).hasSize(1);
		assertThat(details.getBody().items().get(0).ingredientName()).isEqualTo(toNormalizedName("Kylling-" + suffix));
	}

	@Test
	void deleteIngredient_usedByRecipe_returns409Conflict() {
		var suffix = UUID.randomUUID().toString().substring(0, 8);
		var recipeName = "Delete ing test " + suffix;

		// lag oppskrift som bruker ingrediens
		var ingName = "Brukt-ingrediens-" + suffix;
		var createReq = new CreateRecipeRequest(
				recipeName,
				"Ingrediensen skal ikke kunne slettes",
				List.of(new CreateRecipeItemRequest(ingName, new BigDecimal("1"), "stk", null))
		);

		var created = http.postForEntity("/recipes", createReq, CreateRecipeResponse.class);
		assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

		var recipeId = created.getBody().id();

		// hent oppskrift for å få ingredientId
		var details = http.getForEntity("/recipes/" + recipeId, RecipeDetailsResponse.class);
		assertThat(details.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(details.getBody()).isNotNull();
		assertThat(details.getBody().items()).hasSize(1);

		var ingredientId = details.getBody().items().get(0).ingredientId();

		// prøv slett ingrediens
		var deleteResp = http.exchange("/ingredients/" + ingredientId, HttpMethod.DELETE, HttpEntity.EMPTY, String.class);
		assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
	}

	private static String toNormalizedName(String input) {
		if (input == null) return null;
		var trimmed = input.trim();
		if (trimmed.isEmpty()) return trimmed;
		return trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1).toLowerCase();
	}
}