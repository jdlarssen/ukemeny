package no.jdl.ukemeny.recipe;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import no.jdl.ukemeny.ingredient.Ingredient;
import java.math.BigDecimal;

@Entity
@Table(name ="recipe_item")
public class RecipeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @NotNull
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal amount;

    @NotBlank
    @Column(nullable = false)
    private String unit;

    private String note;

    protected RecipeItem() {}

    public RecipeItem(Recipe recipe,
                      Ingredient ingredient,
                      BigDecimal amount,
                      String unit,
                      String note) {
        this.recipe = recipe;
        this.ingredient = ingredient;
        this.amount = amount;
        this.unit = unit;
        this.note = note;
    }

    void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }
    public Long getId() { return id; }
    public Recipe getRecipe() { return recipe; }
    public Ingredient getIngredient() { return ingredient; }
    public BigDecimal getAmount() { return amount; }
    public String getUnit() { return unit; }
    public String getNote() { return note; }
}
