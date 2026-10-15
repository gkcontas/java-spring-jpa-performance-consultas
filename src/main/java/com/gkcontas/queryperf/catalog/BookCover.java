package com.gkcontas.queryperf.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Present for roughly half the books, which is what makes the inverse side of the
 * association genuinely nullable - and therefore impossible to proxy.
 *
 * <p>The owning side points at {@link CoveredBook} rather than {@link Book}, because
 * {@link Book} deliberately has no cover association at all.
 */
@Entity
@Table(name = "book_cover")
@Getter
@Setter
@NoArgsConstructor
public class BookCover {

    @Id
    @Column(name = "book_id")
    private Long bookId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id")
    private CoveredBook book;

    @Column(name = "dominant_color", nullable = false, length = 20)
    private String dominantColor;

    @Column(nullable = false)
    private int width;

    @Column(nullable = false)
    private int height;
}
