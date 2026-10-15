package com.gkcontas.queryperf.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Immutable;

/**
 * The {@code book} table a third time, with {@code @BatchSize} on both collections.
 *
 * <p>{@code @BatchSize} is read when Hibernate builds the collection loader at startup,
 * so it cannot be varied per query. {@code Session.setFetchBatchSize} looks like the
 * runtime equivalent and is not: with it set to 50 the collections still come back one
 * statement at a time, which is measured in the scenario. Mapping the table again with
 * the annotation present is therefore the only way to put batched and unbatched
 * fetching under the same measurement in the same run.
 *
 * <p>The collections are unidirectional with a read-only join column, because the
 * inverse of {@code Review.book} already belongs to {@link Book} and a second
 * {@code mappedBy} onto it is not possible.
 */
@Entity
@Table(name = "book")
@Immutable
@Getter
@Setter
@NoArgsConstructor
public class BatchedBook {

    @Id
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", insertable = false, updatable = false)
    @BatchSize(size = BATCH_SIZE)
    private List<LeanReview> reviews = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "book_author",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "author_id"))
    @BatchSize(size = BATCH_SIZE)
    private List<Author> authors = new ArrayList<>();

    /**
     * Fifty, and the number is a trade-off rather than a tuning knob. Larger means
     * fewer round trips and a longer {@code IN} list, and every distinct list length
     * Hibernate pads to is another entry in the PostgreSQL plan cache.
     */
    public static final int BATCH_SIZE = 50;
}
