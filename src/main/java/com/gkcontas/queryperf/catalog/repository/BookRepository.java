package com.gkcontas.queryperf.catalog.repository;

import com.gkcontas.queryperf.catalog.Book;
import com.gkcontas.queryperf.projection.BookSummary;
import com.gkcontas.queryperf.projection.BookSummaryDto;
import com.gkcontas.queryperf.projection.BookSummaryOpen;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

/**
 * Every variant the scenarios compare, written out as a separate method.
 *
 * <p>Deliberately not a tidy repository. A real one would keep two or three of these;
 * the whole set lives here so the variants can be read next to each other and so that
 * nothing is hidden behind a dynamic specification.
 */
public interface BookRepository extends JpaRepository<Book, Long> {

    /** Nothing fetched. The baseline every repair is measured against. */
    @Query("select b from Book b order by b.id")
    List<Book> findAllPlain();

    /** The same list, with the to-one fetched by an entity graph. */
    @EntityGraph(attributePaths = "publisher")
    @Query("select b from Book b order by b.id")
    List<Book> findAllWithPublisherGraph();

    /** The same list, with the to-one fetched by an explicit join in the query. */
    @Query("select b from Book b join fetch b.publisher order by b.id")
    List<Book> findAllJoinFetchPublisher();

    /** The same answer without entities: three columns, named in the query. */
    @Query("""
            select new com.gkcontas.queryperf.projection.BookSummaryDto(
                b.title, b.publishedYear, b.publisher.name)
            from Book b order by b.id
            """)
    List<BookSummaryDto> findAllSummaryDto();

    /** Closed interface projection; Spring Data narrows the select itself. */
    /**
     * Closed interface projection over a query that selects the entity - which is not a
     * projection. Spring Data narrows the select only when it builds the query itself;
     * handed {@code select b from Book b} it loads every {@code Book} and maps the
     * interface over the managed entities afterwards.
     */
    @Query("select b from Book b order by b.id")
    List<BookSummary> findAllSummaryClosedOverEntityQuery();

    /** The same projection on a derived method, where Spring Data writes the select. */
    List<BookSummary> findByPublishedYearGreaterThanOrderById(int publishedYear);

    /** The open projection on a derived method, for the comparison that matters. */
    List<BookSummaryOpen> findByPublishedYearGreaterThanOrderByIdAsc(int publishedYear);

    /** All books with their reviews fetched by an entity graph: one statement. */
    @EntityGraph(attributePaths = "reviews")
    @Query("select b from Book b order by b.id")
    List<Book> findAllWithReviewsGraph();

    /** All books with their reviews fetched by an explicit join: one statement. */
    @Query("select b from Book b left join fetch b.reviews order by b.id")
    List<Book> findAllJoinFetchReviews();

    /**
     * Reviews paged in an order that is not the foreign key, which is what a feed
     * sorted by anything other than the book does. A page of it spans as many books as
     * it has rows, so an N+1 on the book is not capped by the number of distinct books
     * the way one ordered by id is.
     */
    @Query("select b from Book b order by b.title")
    List<Book> findAllByTitle();

    /**
     * A page of books with their reviews fetched in the same query. This is the method
     * that provokes {@code HHH90003004}: Hibernate cannot apply a row limit to a query
     * whose rows are a collection, so it applies it after loading everything.
     */
    @Query(value = "select b from Book b left join fetch b.reviews",
            countQuery = "select count(b) from Book b")
    Page<Book> findPageJoinFetchReviews(Pageable pageable);

    /** Step one of the two-query page: the ids only, which a limit can be applied to. */
    @Query("select b.id from Book b order by b.id")
    List<Long> findIdPage(Pageable pageable);

    /** Step two: the books of that page, with the collection, in one query. */
    @Query("select b from Book b left join fetch b.reviews where b.id in :ids order by b.id")
    List<Book> findWithReviewsByIds(@Param("ids") List<Long> ids);

    /** A plain page, for the variants that fix the fetching on the session instead. */
    @Query(value = "select b from Book b order by b.id",
            countQuery = "select count(b) from Book b")
    Page<Book> findPagePlain(Pageable pageable);

    /** Two bags fetched at once. Does not run; the scenario reports why. */
    @Query("select b from Book b join fetch b.authors join fetch b.reviews where b.id = :id")
    List<Book> findWithAuthorsAndReviews(@Param("id") long id);

    @Query("select b from Book b join fetch b.authors where b.id = :id")
    List<Book> findWithAuthors(@Param("id") long id);

    @Query("select b from Book b left join fetch b.reviews where b.id = :id")
    List<Book> findWithReviews(@Param("id") long id);

    /**
     * The rows a double fetch join would have to carry: one per combination of author
     * and review. Selecting the ids instead of the entities makes the row count
     * observable, which is the number the cartesian product is actually about.
     */
    @Query("select b.id, a.id, r.id from Book b join b.authors a join b.reviews r where b.id = :id")
    List<Object[]> findJoinRows(@Param("id") long id);

    /**
     * The same list again, cached by the query cache. Enabled per query, never
     * globally: a query cache over entities that are not themselves cached turns one
     * statement into one plus the number of rows.
     */
    @QueryHints(@jakarta.persistence.QueryHint(name = "org.hibernate.cacheable", value = "true"))
    @Query("select b from Book b where b.publishedYear = :year order by b.id")
    List<Book> findByYearCached(@Param("year") int year);

    @Query("select b from Book b where b.publishedYear = :year order by b.id")
    List<Book> findByYearUncached(@Param("year") int year);
}
