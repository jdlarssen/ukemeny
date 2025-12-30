package no.jdl.ukemeny.weeklymenu;

import jakarta.persistence.*;
import no.jdl.ukemeny.recipe.Recipe;

@Entity
@Table(name = "weekly_menu_entry")
public class WeeklyMenuEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "weekly_menu_id", nullable = false)
    private WeeklyMenu weeklyMenu;

    @Column(name = "day_of_week", nullable = false)
    private int dayOfWeek; // 1=Monday ... 7=Sunday

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    private String note;

    @Column(name = "locked", nullable = false)
    private boolean locked = false;

    protected WeeklyMenuEntry() {
    }

    public WeeklyMenuEntry(int dayOfWeek, Recipe recipe, String note) {
        this.dayOfWeek = dayOfWeek;
        this.recipe = recipe;
        this.note = note;
    }

    void setWeeklyMenu(WeeklyMenu weeklyMenu) {
        this.weeklyMenu = weeklyMenu;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Long getId() {
        return id;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public String getNote() {
        return note;
    }
}