package no.jdl.ukemeny.ingredient;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IngredientService {

    private final IngredientRepository repo;

    public IngredientService(IngredientRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public Ingredient getOrCreate(String name) {
        var normalized = normalizeName(name);

        return repo.findByNameIgnoreCase(normalized)
                .orElseGet(() -> {
                    try {
                        return repo.save(new Ingredient(normalized, null));
                    } catch (DataIntegrityViolationException e) {
                        // Race condition: noen andre rakk å opprette den først
                        return repo.findByNameIgnoreCase(normalized)
                                .orElseThrow(() -> e);
                    }
                });
    }

    private String normalizeName(String input) {
        if (input == null) return null;
        var trimmed = input.trim();
        if (trimmed.isEmpty()) return trimmed;

        // MVP: gjør kun "pent" visningsnavn, men bevarer ordet.
        return trimmed.substring(0,1).toUpperCase() + trimmed.substring(1).toLowerCase();
    }
}
