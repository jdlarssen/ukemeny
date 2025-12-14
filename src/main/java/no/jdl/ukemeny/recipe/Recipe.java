package no.jdl.ukemeny.recipe;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;
import java.time.Instant;

@Entity
@Table(name = "recipe")
public class Recipe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    private String description;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Recipe() {
        //JPA
    }
    public Recipe(String name, String description){
        this.name = name;
        this.description = description;
    }
    public void addItem(RecipeItem item) {
        items.add(item);
        item.setRecipe(this);
    }

    public List<RecipeItem> getItems() {
        return items;
    }
    @PreUpdate
    void preUpdate(){
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void clearItems(){
        items.clear();
    }

    void setName(String name) { this.name = name; }
    void setDescription(String description) { this.description = description; }
}