package no.jdl.ukemeny.ingredient;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "category")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 1000;

    protected Category() {}

    public Category(String name) {
        this.name = name;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public int getSortOrder() { return sortOrder; }

    public void setName(String name) {          // <-- legg til
        this.name = name;
    }

    public void setSortOrder(int sortOrder) {   // <-- gjør public
        this.sortOrder = sortOrder;
    }
}