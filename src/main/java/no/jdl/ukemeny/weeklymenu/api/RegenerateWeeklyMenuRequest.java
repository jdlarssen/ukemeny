// src/main/java/no/jdl/ukemeny/weeklymenu/api/RegenerateWeeklyMenuRequest.java
package no.jdl.ukemeny.weeklymenu.api;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record RegenerateWeeklyMenuRequest(
        @NotNull LocalDate weekStartDate
) {}