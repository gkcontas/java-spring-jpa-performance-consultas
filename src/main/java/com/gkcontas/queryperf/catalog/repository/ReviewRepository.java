package com.gkcontas.queryperf.catalog.repository;

import com.gkcontas.queryperf.catalog.Review;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * {@code Review.book} is on the JPA default, which is eager.
     *
     * <p>Ordered by reviewer rather than by the foreign key on purpose: ordered by
     * {@code book_id} a page of reviews covers a handful of books and the eager fetch
     * looks almost free, because the session already has each book after the first row
     * of it. Ordered by anything else - which is what a real feed does - each row is a
     * different book.
     */
    @Query("select r from Review r order by r.reviewerName, r.id")
    List<Review> findPageSpread(Pageable pageable);

    /** The same page with the book fetched in the query rather than row by row. */
    @Query("select r from Review r join fetch r.book order by r.reviewerName, r.id")
    List<Review> findPageSpreadJoinFetch(Pageable pageable);
}
