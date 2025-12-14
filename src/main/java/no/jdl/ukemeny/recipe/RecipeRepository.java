package no.jdl.ukemeny.recipe;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    List<Recipe> findTop10ByNameContainingIgnoreCaseOrderByIdDesc(String name);
    @Query("""
        select distinct  r from Recipe r
        left join fetch r.items i
        left join fetch i.ingredient
        where r.id = :id
        """)
    Optional<Recipe> findByIdWithItems(Long id);

    @Query("select r.id from Recipe r")
    java.util.List<Long> findAllIds();

    @Query("""
        select distinct r from Recipe r
        left join fetch r.items i
        left join fetch i.ingredient ing
        where r.id in :ids
        """)
    List<Recipe> findAllByIdWithItems(java.util.Collection<Long> ids);
}