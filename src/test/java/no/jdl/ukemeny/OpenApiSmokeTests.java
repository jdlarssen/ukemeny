package no.jdl.ukemeny;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiSmokeTests {

    @Autowired TestRestTemplate http;

    @Test
    void openApiDocs_isAvailable() {
        var res = http.getForEntity("/v3/api-docs", String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).contains("\"openapi\"");
    }

    @Test
    void swaggerUi_isAvailable() {
        var res = http.getForEntity("/swagger-ui/index.html", String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).contains("Swagger UI");
    }
}