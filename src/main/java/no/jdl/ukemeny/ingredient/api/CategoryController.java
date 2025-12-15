package no.jdl.ukemeny.ingredient.api;

import no.jdl.ukemeny.ingredient.CategoryRepository;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
public class CategoryController {
    private final CategoryRepository repo;

    public CategoryController(CategoryRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<CategoryResponse> list() {
        return repo.findAllByOrderBySortOrderAscNameAsc().stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getSortOrder()))
                .toList();
    }
}