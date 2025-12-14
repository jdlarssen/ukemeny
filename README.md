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
- Ukemeny:
    - Opprette ukemeny manuelt
    - Generere ukemeny automatisk for en uke (mandag som startdato)
    - Forsøker å variere fra forrige uke når mulig
- Handleliste:
    - Generere handleliste fra en ukemeny
    - Summerer ingredienser på tvers av ukens middager (skiller per unit)

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
- `src/main/resources/application.properties`

### 3) Start appen
```bash
./mvnw spring-boot:run
```
## Endepunkter (MVP)
### Health
```bash 
curl -i http://localhost:8080/actuator/health
```
### Recipes
#### Opprett:
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
#### Søk (Top 10):
```bash
curl "http://localhost:8080/recipes?name=ta"
```
#### Hent:
```bash 
curl "http://localhost:8080/recipes/1"
```
#### Oppdater:
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
#### Slett:
```bash
curl -i -X DELETE http://localhost:8080/recipes/1
```

### Weekly menus
#### Generer automatisk:
```bash
curl -i -X POST "http://localhost:8080/weekly-menus/generate" \
  -H "Content-Type: application/json" \
  -d '{ "weekStartDate": "2025-12-22" }'
```
#### Hent:
```bash 
curl "http://localhost:8080/weekly-menus/4"
```
#### Handleliste:
```bash
curl "http://localhost:8080/weekly-menus/4/shopping-list"
```

## Notater / MVP-avgrensninger
- Kun middag (ingen frokost/lunsj)
- Porsjoner er ikke modellert i MVP (antatt 2 porsjoner)
- Unikhet for ingrediens håndheves i database (case-insensitive håndteres i service)
- Ukemeny kan ha repeats hvis det fins færre enn 7 oppskrifter

## Plan videre
- A: Handleliste "forklarbarhet" (hvilke oppskrifter/dager bidro til hver linje)
- B: Kategorisering og gruppering i handleliste 
- C: Låsing av dager (manuelt valg) + autogenerer resten