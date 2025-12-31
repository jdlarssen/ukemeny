package no.jdl.ukemeny.ingredient;

import no.jdl.ukemeny.common.NotFoundException;
import no.jdl.ukemeny.ingredient.api.UpdateCategoryRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.CONFLICT;

@Service
public class CategoryService {

    private final CategoryRepository repo;

    public CategoryService(CategoryRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public void patch(Long id, UpdateCategoryRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("request body is required");
        }

        boolean hasAnyChange = (req.name() != null) || (req.sortOrder() != null);
        if (!hasAnyChange) {
            throw new IllegalArgumentException("Nothing to update");
        }

        var category = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found: " + id));

        if (req.name() != null) {
            var trimmed = req.name().trim();
            category.setName(trimmed);
        }

        if (req.sortOrder() != null) {
            category.setSortOrder(req.sortOrder());
        }

        if (req.sortOrder() != null && req.sortOrder() < 0) {
            throw new IllegalArgumentException("sortOrder must be >= 0");
        }

        // Tving flush så du får feil her og ikke "senere"
        // DB-en din har case-insensitive unique index -> kan gi DataIntegrityViolationException ved dup.
        try {
            repo.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(CONFLICT, "Category name already exists (case-insensitive)", e);
        }
    }
}