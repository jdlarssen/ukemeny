package no.jdl.ukemeny.weeklymenu;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;

public interface WeeklyMenuRepository  extends JpaRepository<WeeklyMenu, Long> {
    @Query("""
        select wm from WeeklyMenu wm
        where wm.weekStartDate < :weekStartDate
        order by wm.weekStartDate desc, wm.id desc
        """)
    java.util.List<WeeklyMenu> findPrevious(LocalDate weekStartDate, Pageable pageable);

    @Query("""
        select distinct wm from WeeklyMenu wm
        left join fetch wm.entries e
        left join fetch e.recipe
        where wm.id = :id
        """)
    Optional<WeeklyMenu> findByIdWithEntries(Long id);
}