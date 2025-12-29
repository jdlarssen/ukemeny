package no.jdl.ukemeny.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI ukemenyOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ukemeny API (MVP)")
                        .description("""
                                Backend-API for oppskrifter, ukemeny og handleliste.

                                Inkluderer:
                                - Recipes: CRUD-ish + søk
                                - Weekly menus: opprett og generer
                                - Shopping list: aggregert og gruppert per kategori
                                - Ingredients: list + kategori (single/bulk) + delete hvis ubrukt
                                """)
                        .version("0.0.1-SNAPSHOT")
                        .contact(new Contact().name("Ukemeny"))
                );
    }
}