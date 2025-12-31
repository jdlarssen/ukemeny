package no.jdl.ukemeny.ingredient;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

    @Query("""
        select i
        from Ingredient i 
        join fetch i.category c
        order by c.sortOrder asc, lower(i.name) asc
    """)
    List<Ingredient> findAllWithCategoryOrdered();
    Optional<Ingredient> findByNameIgnoreCase(String name);

    @Query("""
        select (count(ri) > 0 )
        from RecipeItem ri
        where ri.ingredient.id = :ingredientId
    """)
    boolean isUsedInAnyRecipe(@Param("ingredientId") Long ingredientId);

    @Query("""
        select distinct ri.ingredient.id
        from RecipeItem ri
    """)
    List<Long> findUsedIngredientIds();
}