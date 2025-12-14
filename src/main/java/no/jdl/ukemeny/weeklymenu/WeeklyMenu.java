package no.jdl.ukemeny.weeklymenu;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "weekly_menu")
public class WeeklyMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @OneToMany(mappedBy = "weeklyMenu", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WeeklyMenuEntry> entries = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected WeeklyMenu() {}

    public WeeklyMenu(LocalDate weekStartDate) {
        this.weekStartDate = weekStartDate;
    }

    public void addEntry(WeeklyMenuEntry entry) {
        entries.add(entry);
        entry.setWeeklyMenu(this);
    }

    public void clearEntries(){
        entries.clear();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public LocalDate getWeekStartDate () { return weekStartDate; }
    public List<WeeklyMenuEntry> getEntries() { return entries; }
}
