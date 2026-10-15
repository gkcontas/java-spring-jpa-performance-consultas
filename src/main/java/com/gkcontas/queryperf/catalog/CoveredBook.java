package com.gkcontas.queryperf.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

/**
 * The {@code book} table again, with the one association {@link Book} refuses to carry:
 * the inverse side of the optional one-to-one to {@code book_cover}.
 *
 * <p>Annotated {@code LAZY}, and not lazy. On the inverse side the foreign key lives in
 * the other table, so Hibernate cannot tell from the book row whether there is a cover;
 * it has to choose between installing a proxy and writing {@code null}, and the only
 * way to choose is to go and look. Without bytecode enhancement that is one statement
 * per book, before anybody touches {@code getCover()}.
 *
 * <p>Read-only, and separated from {@link Book} for the sake of every other measurement
 * in the project: with this mapping on the main entity, the scenarios that get down to
 * a single statement could never get below one plus the number of rows.
 */
@Entity
@Table(name = "book")
@Immutable
@Getter
@Setter
@NoArgsConstructor
public class CoveredBook {

    @Id
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @OneToOne(mappedBy = "book", fetch = FetchType.LAZY)
    private BookCover cover;
}
