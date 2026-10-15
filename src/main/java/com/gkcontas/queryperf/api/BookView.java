package com.gkcontas.queryperf.api;

import java.math.BigDecimal;
import java.util.List;

public record BookView(
        long id,
        String title,
        String isbn,
        int publishedYear,
        BigDecimal price,
        String format,
        int reviewCount) {

    public record Detail(
            long id,
            String title,
            String isbn,
            int publishedYear,
            BigDecimal price,
            String format,
            String publisherName,
            List<String> authors,
            int reviewCount,
            double averageRating) {}
}
