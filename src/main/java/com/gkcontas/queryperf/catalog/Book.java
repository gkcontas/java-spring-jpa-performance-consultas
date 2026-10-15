package com.gkcontas.queryperf.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The aggregate every scenario queries.
 *
 * <p>Deliberately annotated the way a careful but unmeasured developer would annotate
 * it: every association lazy, nothing batched, nothing cached. That is the baseline the
 * fixes are measured against, and none of the fixes needs this class changed - they are
 * all either query-side ({@code @EntityGraph}, fetch join, projection) or session-side
 * ({@code setFetchBatchSize}, {@code setSubselectFetchingEnabled}).
 *
 * <p>It has no association to {@code book_cover}. The inverse side of a one-to-one is
 * not lazy without bytecode enhancement, so one here would add a statement per book to
 * every single measurement in the project - including the ones that are supposed to be
 * down to one. {@link CoveredBook} maps the same table with that association added, and
 * the scenario {@code inverse-one-to-one} measures what it costs.
 *
 * <p>No Lombok {@code @Data}, {@code @ToString} or {@code @EqualsAndHashCode} here on
 * purpose. All three walk every field, so one of them on an entity with lazy
 * associations turns a log line or a {@code HashSet} insertion into an N+1 that no
 * query in the codebase asked for.
 */
@Entity
@Table(name = "book")
@Getter
@Setter
@NoArgsConstructor
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 20, unique = true)
    private String isbn;

    @Column(name = "published_year", nullable = false)
    private int publishedYear;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Format format;

    /**
     * {@code LAZY} has to be written out. {@code @ManyToOne} and {@code @OneToOne}
     * default to {@code EAGER} in JPA, and an eager to-one is an N+1 you get for free
     * on any {@code findAll} - see {@link Review}, which is left on the default to
     * measure it.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publisher_id", nullable = false)
    private Publisher publisher;

    /**
     * A {@code List}, which Hibernate maps as a bag. Two bags fetched in one query
     * raise {@code MultipleBagFetchException}; the usual "fix" of turning both into
     * {@code Set} only hides the reason the exception exists, which is the cartesian
     * product underneath.
     */
    @OneToMany(mappedBy = "book", fetch = FetchType.LAZY)
    private List<Review> reviews = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "book_author",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "author_id"))
    private List<Author> authors = new ArrayList<>();
}
