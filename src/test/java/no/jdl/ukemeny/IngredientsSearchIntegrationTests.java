package no.jdl.ukemeny;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IngredientsSearchIntegrationTests {

    @Autowired TestRestTemplate http;
    @Autowired ObjectMapper om;

    @Test
    void ingredients_unusedFilter_partitionsTheList() throws Exception {
        // all
        JsonNode all = getJson("/ingredients");
        int allCount = all.size();

        // unused + used
        JsonNode unused = getJson("/ingredients?unused=true");
        JsonNode used = getJson("/ingredients?unused=false");

        assertThat(unused.size() + used.size()).isEqualTo(allCount);

        // ingen overlap på id
        var ids = new HashSet<Long>();
        for (JsonNode n : unused) ids.add(n.get("id").asLong());
        for (JsonNode n : used) {
            long id = n.get("id").asLong();
            assertThat(ids).as("Ingredient id should not be both unused and used: " + id).doesNotContain(id);
        }
    }

    private JsonNode getJson(String path) throws Exception {
        ResponseEntity<String> res = http.getForEntity(path, String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        return om.readTree(res.getBody());
    }
}