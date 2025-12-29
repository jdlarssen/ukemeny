# Ukemeny API (MVP)

Dette prosjektet er en backend (API) for å:
- lagre oppskrifter (recipes) med ingredienser
- generere ukemeny automatisk
- generere handleliste basert på ukemeny

Målet er å bygge en enkel, fungerende MVP først, og deretter forbedre med bedre variasjon, forklarbarhet (hvorfor ingredienser havner i handlelisten), og låsing/manuell overstyring av dager.

## Status
- CRUD-ish for oppskrifter:
  - Opprette oppskrift med items (ingredient opprettes automatisk hvis den ikke finnes)
  - Hente oppskrift (inkl. items)
  - Oppdatere oppskrift (replace items)
  - Slette oppskrift
  - Søk på oppskrifter (Top 10)
- Ingredienser:
  - GET /ingredients (sortert etter category.sortOrder + navn)
  - PATCH /ingredients/{id}/category
  - PATCH /ingredients/category (bulk)
  - DELETE /ingredients/{id} (409 hvis brukt i oppskrift)
- Ukemeny:
  - Opprette ukemeny manuelt (entries støtter `locked`)
  - Generere ukemeny automatisk for en uke (mandag som startdato)
  - Forsøker å variere fra forrige uke når mulig
- Handleliste:
  - Generere handleliste fra en ukemeny
  - Summerer ingredienser på tvers av ukens middager (skiller per unit)
  - Grupperer og sorterer etter kategori (category.sortOrder)
- Kategorier:
  - GET /categories (sortert etter sortOrder)

## Teknologi
- Java 21 (Temurin)
- Spring Boot
- PostgreSQL (Docker)
- Flyway migrations
- Maven

## Krav
- Docker + Docker Compose
- Java 21

## Kom i gang
Appen kjører på http://localhost:8080.

### 1) Start database
```bash
docker compose up -d
docker compose ps
```
### 2) Konfig (lokalt)
Appen bruker Postgres i Docker. Konfig ligger i:
- `src/main/resources/application.yml`

Datasource konfigureres med environment variables.
Lag en '.env' lokalt, f.eks:
```.env
DB_URL=jdbc:postgresql://localhost:5432/ukemeny
DB_USERNAME=ukemeny
DB_PASSWORD=ukemeny
```
### 3) Start appen
```bash
./mvnw spring-boot:run
```

## Testing
Testene kjører mot Postgres og bruker Flyway-migrasjoner.

### Lokalt
1) Start database: 
```bash
docker compose up-d
```
2) Kjør tester:
```bash
./mvnw test
```
### CI (GitHub Actions)
Workflow starter Postgres som en service-container og kjører
```bash
./mvnw test
```
Test-profilen ligger i:
- `src/test/resources/application-test.properties`

## Endepunkter (MVP)
### Health
```bash 
curl -i http://localhost:8080/actuator/health
```
### Recipes
#### Opprett
```bash 
curl -i -X POST http://localhost:8080/recipes \
  -H "Content-Type: application/json" \
  -d '{
    "name":"Taco",
    "description":"Rask middag",
    "items":[
      {"ingredientName":"Kjøttdeig","amount":400,"unit":"g","note":null},
      {"ingredientName":"Tacokrydder","amount":1,"unit":"pose","note":null}
    ]
  }'

```
#### Søk (Top 10)
```bash
curl "http://localhost:8080/recipes?name=ta"
```
#### Hent
```bash 
curl "http://localhost:8080/recipes/1"
```
#### Oppdater
```bash
curl -i -X PUT http://localhost:8080/recipes/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name":"Taco (oppdatert)",
    "description":"Ny beskrivelse",
    "items":[
      {"ingredientName":"Kjøttdeig","amount":500,"unit":"g","note":null}
    ]
  }'

```
#### Slett
```bash
curl -i -X DELETE http://localhost:8080/recipes/1
```

### Weekly menus
#### Opprett manuelt (med locked)
```bash
curl -i -X POST "http://localhost:8080/weekly-menus" \
  -H "Content-Type: application/json" \
  -d '{
    "weekStartDate": "2025-12-22",
    "dinners": [
      { "dayOfWeek": 1, "recipeId": 26, "locked": true, "note": null }
    ]
  }'
```
#### Generer automatisk
```bash
curl -i -X POST "http://localhost:8080/weekly-menus/generate" \
  -H "Content-Type: application/json" \
  -d '{ "weekStartDate": "2025-12-22" }'
```
#### Hent
```bash 
curl "http://localhost:8080/weekly-menus/4"
```
#### Handleliste
```bash
curl "http://localhost:8080/weekly-menus/4/shopping-list"
```
### Categories
```bash
curl "http://localhost:8080/categories"
```
### Ingredients
#### List
```bash
curl "http://localhost:8080/ingredients"
```
#### Sett kategori (single)
```bash
curl -i -X PATCH "http://localhost:8080/ingredients/5/category" \
  -H "Content-Type: application/json" \
  -d '{ "categoryId": 12 }'
```
#### Sett kategori (bulk)
```bash
curl -i -X PATCH "http://localhost:8080/ingredients/category" \
  -H "Content-Type: application/json" \
  -d '{
    "updates": [
      { "ingredientId": 6, "categoryId": 7 },
      { "ingredientId": 4, "categoryId": 6 }
    ]
  }'
```
#### Slett (kun hvis ubrukt)
```bash
curl -i -X DELETE "http://localhost:8080/ingredients/<ID>"
```

## Notater / MVP-avgrensninger
- Kun middag (ingen frokost/lunsj)
  - Porsjoner er ikke modellert i MVP (antatt 2 porsjoner)
  - Unikhet for ingrediens håndheves i database (case-insensitive håndteres i service)
  - Ukemeny kan ha repeats hvis det fins færre enn 7 oppskrifter

## Plan videre
- A: Swagger/OpenAPI
- B: Mer testdekning (shopping list + weekly menu edge cases)
- C: Ukemeny: regenerer kun ulåst dager (locked)
- D: Admin/vedlikehold: rydde i ingredienser (bulk-oppdatering + sletting)
```makefile
::contentReference[oaicite:0]{index=0}
```