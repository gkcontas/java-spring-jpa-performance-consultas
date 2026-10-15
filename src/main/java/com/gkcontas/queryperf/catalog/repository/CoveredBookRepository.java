package com.gkcontas.queryperf.catalog.repository;

import com.gkcontas.queryperf.catalog.CoveredBook;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CoveredBookRepository extends JpaRepository<CoveredBook, Long> {

    /** Nothing touched, and still one statement per book, for the cover. */
    @Query("select b from CoveredBook b order by b.id")
    List<CoveredBook> findAllPlain();

    /** The cover brought along in the same statement by a left join. */
    @EntityGraph(attributePaths = "cover")
    @Query("select b from CoveredBook b order by b.id")
    List<CoveredBook> findAllWithCoverGraph();
}
