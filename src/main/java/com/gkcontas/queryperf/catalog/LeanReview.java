package com.gkcontas.queryperf.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

/**
 * The same {@code review} table as {@link Review}, mapped a second time with the only
 * difference that matters: {@code fetch = LAZY} on the to-one.
 *
 * <p>Two entities over one table is unusual in an application, and on purpose here. A
 * fetch type is a compile-time decision, so the only way to put the two versions of it
 * under the same measurement in the same run is to map the table twice. Read-only, so
 * nothing can be written through the duplicate mapping.
 */
@Entity
@Table(name = "review")
@Immutable
@Getter
@Setter
@NoArgsConstructor
public class LeanReview {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "reviewer_name", nullable = false, length = 120)
    private String reviewerName;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
