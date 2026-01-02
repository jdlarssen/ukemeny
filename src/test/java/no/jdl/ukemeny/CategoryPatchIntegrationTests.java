package no.jdl.ukemeny;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CategoryPatchIntegrationTests {

    @Autowired TestRestTemplate http;
    @Autowired ObjectMapper om;
    @Autowired no.jdl.ukemeny.ingredient.CategoryRepository categoryRepo;


    @Test
    void patchCategory_updatesSortOrder_andIsReflectedInList() throws Exception {
        JsonNode before = getJson("/categories");
        assertThat(before.isArray()).isTrue();
        assertThat(before.size()).isGreaterThanOrEqualTo(2);

        long id = before.get(0).get("id").asLong();
        int oldSortOrder = before.get(0).get("sortOrder").asInt();

        int newSortOrder = oldSortOrder + 123; // garantert endring

        patchJsonExpect("/categories/" + id, Map.of("sortOrder", newSortOrder), HttpStatus.NO_CONTENT);

        JsonNode after = getJson("/categories");
        JsonNode updated = findById(after, id);

        assertThat(updated).as("Oppdatert kategori må finnes i GET /categories").isNotNull();
        assertThat(updated.get("sortOrder").asInt()).isEqualTo(newSortOrder);
    }

    @Test
    void patchCategory_unknownId_returns404() throws Exception {
        // Viktig: må sende et gyldig patch-body, ellers får du 400 ("Nothing to update")
        patchJsonExpect("/categories/99999999", Map.of("sortOrder", 10), HttpStatus.NOT_FOUND);
    }

    @Test
    void patchCategory_renameToExistingName_caseInsensitive_returns409() throws Exception {
        JsonNode cats = getJson("/categories");
        assertThat(cats.size()).isGreaterThanOrEqualTo(2);

        long idA = cats.get(0).get("id").asLong();
        long idB = cats.get(1).get("id").asLong();
        String nameB = cats.get(1).get("name").asText();

        // Forsøk å rename A til samme navn som B (men med annen casing)
        String collidingName = nameB.toUpperCase(Locale.ROOT);

        // Hvis A allerede heter nameB, bytt rolle
        if (cats.get(0).get("name").asText().equalsIgnoreCase(nameB)) {
            idA = cats.get(1).get("id").asLong();
            idB = cats.get(0).get("id").asLong();
            nameB = cats.get(0).get("name").asText();
            collidingName = nameB.toUpperCase(Locale.ROOT);
        }

        // PATCH med name (må være non-blank)
        patchJsonExpect("/categories/" + idA, Map.of("name", collidingName), HttpStatus.CONFLICT);
    }
    @Test
    void patchCategory_emptyBody_returns400() throws Exception {
        JsonNode cats = getJson("/categories");
        long id = cats.get(0).get("id").asLong();

        patchJsonExpect("/categories/" + id, Map.of(), HttpStatus.BAD_REQUEST);
    }
    @Test
    void patchCategory_blankName_returns400() throws Exception {
        JsonNode cats = getJson("/categories");
        long id = cats.get(0).get("id").asLong();

        patchJsonExpect("/categories/" + id, Map.of("name", "   "), HttpStatus.BAD_REQUEST);
    }
    @Test
    void patchCategory_updatesName_only() throws Exception {
        JsonNode before = getJson("/categories");
        long id = before.get(0).get("id").asLong();
        String oldName = before.get(0).get("name").asText();

        String newName = oldName + " X"; // enkel, garantert endring

        patchJsonExpect("/categories/" + id, Map.of("name", newName), HttpStatus.NO_CONTENT);

        JsonNode after = getJson("/categories");
        JsonNode updated = findById(after, id);

        assertThat(updated.get("name").asText()).isEqualTo(newName);
    }
    @Test
    void patchCategory_noBody_returns400() throws Exception {
        JsonNode cats = getJson("/categories");
        long id = cats.get(0).get("id").asLong();

        patchNoBodyExpect("/categories/" + id, HttpStatus.BAD_REQUEST);
    }


    // -------- helpers --------

    private JsonNode getJson(String path) throws Exception {
        ResponseEntity<String> res = http.getForEntity(path, String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        return om.readTree(res.getBody());
    }

    private void patchJsonExpect(String path, Object body, HttpStatus expected) throws Exception {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> req = new HttpEntity<>(om.writeValueAsString(body), h);

        ResponseEntity<String> res = http.exchange(path, HttpMethod.PATCH, req, String.class);

        if (!res.getStatusCode().equals(expected)) {
            throw new AssertionError(
                    "PATCH " + path + " expected " + expected +
                            " but got " + res.getStatusCode() +
                            " body=" + res.getBody()
            );
        }
    }

    private JsonNode findById(JsonNode array, long id) {
        for (JsonNode n : array) {
            if (n.hasNonNull("id") && n.get("id").asLong() == id) {
                return n;
            }
        }
        return null;
    }
    private void patchNoBodyExpect(String path, HttpStatus expected) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> req = new HttpEntity<>(null, h);

        ResponseEntity<String> res = http.exchange(path, HttpMethod.PATCH, req, String.class);

        if (!res.getStatusCode().equals(expected)) {
            throw new AssertionError(
                    "PATCH " + path + " expected " + expected +
                            " but got " + res.getStatusCode() +
                            " body=" + res.getBody()
            );
        }
    }
    private void assertCategoriesSortedBySortOrder(JsonNode categories) {
        // categories = arrayen fra /weekly-menus/{id}/shopping-list (root.get("categories"))
        List<String> namesInOrder = new ArrayList<>();
        for (JsonNode c : categories) {
            // I shopping-list response heter feltet typisk "categoryName"
            String name = c.get("categoryName").asText();
            namesInOrder.add(name);
        }

        // Hent sortOrder fra DB (via CategoryRepository)
        List<Integer> sortOrdersInOrder = new ArrayList<>();
        for (String name : namesInOrder) {
            int sort = categoryRepo.findByNameIgnoreCase(name)
                    .orElseThrow(() -> new AssertionError("Category not found in DB: " + name))
                    .getSortOrder();
            sortOrdersInOrder.add(sort);
        }

        // Verifiser sortering: sortOrder asc, og ved likhet name asc (case-insensitive)
        for (int i = 1; i < namesInOrder.size(); i++) {
            int prevSort = sortOrdersInOrder.get(i - 1);
            int currSort = sortOrdersInOrder.get(i);

            assertThat(prevSort).isLessThanOrEqualTo(currSort);

            if (prevSort == currSort) {
                String prevName = namesInOrder.get(i - 1);
                String currName = namesInOrder.get(i);
                assertThat(prevName.compareToIgnoreCase(currName)).isLessThanOrEqualTo(0);
            }
        }
    }
}