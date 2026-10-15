package com.gkcontas.queryperf.catalog.repository;

import com.gkcontas.queryperf.catalog.LeanReview;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LeanReviewRepository extends JpaRepository<LeanReview, Long> {

    /** The same rows as {@link ReviewRepository#findPageSpread}, with a lazy to-one. */
    @Query("select r from LeanReview r order by r.reviewerName, r.id")
    List<LeanReview> findPageSpread(Pageable pageable);
}
