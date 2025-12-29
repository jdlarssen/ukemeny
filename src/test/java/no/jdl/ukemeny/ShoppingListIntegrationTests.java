package no.jdl.ukemeny;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ShoppingListIntegrationTests {

    @Autowired TestRestTemplate http;
    @Autowired ObjectMapper om;

    @Test
    void shoppingList_groupsByCategorySortOrder_andAggregatesPerIngredientAndUnit_withSortedSources() throws Exception {
        // 1) Hent categoryId’er dynamisk (ikke hardkod id)
        var categoriesJson = getJson("/categories");
        Map<String, Long> categoryIdByName = new HashMap<>();
        for (JsonNode c : categoriesJson) {
            categoryIdByName.put(c.get("name").asText(), c.get("id").asLong());
        }
        Long kjottCatId = categoryIdByName.get("Kjøtt");
        Long krydderCatId = categoryIdByName.get("Krydder & sauser");
        assertThat(kjottCatId).as("Category 'Kjøtt' må finnes").isNotNull();
        assertThat(krydderCatId).as("Category 'Krydder & sauser' må finnes").isNotNull();

        // 2) Lag to oppskrifter med overlappende ingredienser (samme unit)
        long r1 = postRecipe("Taco A", "Test", List.of(
                item("Kjøttdeig", new BigDecimal("400"), "g", null),
                item("Tacokrydder", new BigDecimal("1"), "pose", null)
        ));

        long r2 = postRecipe("Taco B", "Test", List.of(
                item("Kjøttdeig", new BigDecimal("200"), "g", null),
                item("Tacokrydder", new BigDecimal("1"), "pose", null)
        ));

        // 3) Finn ingredientId’er via /ingredients
        var ingredientsJson = getJson("/ingredients");
        Map<String, Long> ingredientIdByName = new HashMap<>();
        for (JsonNode i : ingredientsJson) {
            ingredientIdByName.put(i.get("name").asText(), i.get("id").asLong());
        }
        Long kjottdeigId = ingredientIdByName.get("Kjøttdeig");
        Long tacokrydderId = ingredientIdByName.get("Tacokrydder");
        assertThat(kjottdeigId).as("Ingredient 'Kjøttdeig' må finnes").isNotNull();
        assertThat(tacokrydderId).as("Ingredient 'Tacokrydder' må finnes").isNotNull();

        // 4) Sett riktige kategorier på ingrediensene (bulk)
        patchJson("/ingredients/category", Map.of(
                "updates", List.of(
                        Map.of("ingredientId", kjottdeigId, "categoryId", kjottCatId),
                        Map.of("ingredientId", tacokrydderId, "categoryId", krydderCatId)
                )
        ));

        // 5) Opprett en ukemeny deterministisk (to dager) – viktig for stabil test
        long menuId = postWeeklyMenu(LocalDate.of(2025, 12, 22), List.of(
                dinner(1, r1, false, null),
                dinner(2, r2, false, null)
        ));

        // 6) Hent handlelista
        JsonNode shopping = getJson("/weekly-menus/" + menuId + "/shopping-list");
        assertThat(shopping.get("weeklyMenuId").asLong()).isEqualTo(menuId);

        // categories i riktig rekkefølge (sortOrder): Kjøtt før Krydder & sauser
        JsonNode cats = shopping.get("categories");
        List<String> catNames = new ArrayList<>();
        for (JsonNode c : cats) catNames.add(c.get("category").asText());

        int idxKjott = catNames.indexOf("Kjøtt");
        int idxKrydder = catNames.indexOf("Krydder & sauser");
        assertThat(idxKjott).isGreaterThanOrEqualTo(0);
        assertThat(idxKrydder).isGreaterThanOrEqualTo(0);
        assertThat(idxKjott).isLessThan(idxKrydder);

        // Finn Kjøtt-kategorien og sjekk aggregert amount
        JsonNode kjottCategory = null;
        for (JsonNode c : cats) {
            if ("Kjøtt".equals(c.get("category").asText())) {
                kjottCategory = c;
                break;
            }
        }
        assertThat(kjottCategory).isNotNull();

        // Items i Kjøtt skal inneholde Kjøttdeig med sum 600g
        JsonNode kjottItems = kjottCategory.get("items");
        JsonNode kjottdeig = null;
        for (JsonNode it : kjottItems) {
            if ("Kjøttdeig".equals(it.get("ingredientName").asText())) {
                kjottdeig = it;
                break;
            }
        }
        assertThat(kjottdeig).isNotNull();
        assertThat(kjottdeig.get("unit").asText()).isEqualTo("g");
        assertThat(kjottdeig.get("amount").decimalValue()).isEqualByComparingTo(new BigDecimal("600"));

        // sources skal være sortert på dayOfWeek: 1, 2
        List<Integer> days = new ArrayList<>();
        for (JsonNode s : kjottdeig.get("sources")) {
            days.add(s.get("dayOfWeek").asInt());
        }
        assertThat(days).containsExactly(1, 2);
    }

    // ---------- helpers ----------

    private Map<String, Object> item(String ingredientName, BigDecimal amount, String unit, String note) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ingredientName", ingredientName);
        m.put("amount", amount);
        m.put("unit", unit);
        m.put("note", note);
        return m;
    }

    private Map<String, Object> dinner(int dayOfWeek, long recipeId, boolean locked, String note) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("dayOfWeek", dayOfWeek);
        m.put("recipeId", recipeId);
        m.put("locked", locked);
        m.put("note", note);
        return m;
    }

    private long postRecipe(String name, String description, List<Map<String, Object>> items) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("description", description);
        body.put("items", items);

        JsonNode res = postJson("/recipes", body, HttpStatus.CREATED);
        return res.get("id").asLong();
    }

    private long postWeeklyMenu(LocalDate weekStartDate, List<Map<String, Object>> dinners) throws Exception {
        // Forutsetter at du har POST /weekly-menus (samme som service.create(CreateWeeklyMenuRequest))
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("weekStartDate", weekStartDate.toString());
        body.put("dinners", dinners);

        JsonNode res = postJson("/weekly-menus", body, HttpStatus.CREATED);
        return res.get("id").asLong();
    }

    private JsonNode getJson(String path) throws Exception {
        ResponseEntity<String> res = http.getForEntity(path, String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        return om.readTree(res.getBody());
    }

    private JsonNode postJson(String path, Object body, HttpStatus expected) throws Exception {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> req = new HttpEntity<>(om.writeValueAsString(body), h);

        ResponseEntity<String> res = http.postForEntity(path, req, String.class);

        if (!res.getStatusCode().equals(expected)) {
            throw new AssertionError(
                    "POST " + path + " expected " + expected +
                            " but got " + res.getStatusCode() +
                            " body=" + res.getBody()
            );
        }

        return om.readTree(res.getBody());
    }


    private void patchJson(String path, Object body) throws Exception {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> req = new HttpEntity<>(om.writeValueAsString(body), h);

        ResponseEntity<Void> res = http.exchange(path, HttpMethod.PATCH, req, Void.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
    private LocalDate randomMonday() {
        // 2099-01-04 er en mandag. Legg på et “tilfeldig” antall uker for unikhet.
        int weeks = Math.floorMod(UUID.randomUUID().hashCode(), 5000);
        return LocalDate.of(2099, 1, 4).plusWeeks(weeks);
    }
}