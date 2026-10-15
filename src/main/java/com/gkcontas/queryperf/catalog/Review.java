package com.gkcontas.queryperf.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The {@code book} association is left on the JPA default, which is {@code EAGER}.
 *
 * <p>This is not an oversight, it is the control group. Nothing in the code has to
 * touch {@code getBook()} for the fetch to happen: a plain {@code findAll} on reviews
 * drags one book per distinct book id behind it, and each of those books then has its
 * own un-proxyable cover. {@link LeanReview} maps the same table with the single word
 * {@code LAZY} added, and the scenario {@code eager-to-one} runs the two side by side.
 */
@Entity
@Table(name = "review")
@Getter
@Setter
@NoArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
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
