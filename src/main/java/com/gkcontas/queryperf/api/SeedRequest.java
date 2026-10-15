package com.gkcontas.queryperf.api;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * An override of the seeded shape, for measuring the same scenarios at a different size.
 *
 * <p>Every message is written out. The built-in ones are resolved against the locale of
 * the JVM, which on the machine this was built on is pt-BR - an API whose contract is in
 * English would answer validation errors in Portuguese.
 */
public record SeedRequest(
        @Min(value = 1, message = "publishers must be at least 1")
        @Max(value = 1000, message = "publishers must be at most 1000")
        int publishers,

        @Min(value = 1, message = "authors must be at least 1")
        @Max(value = 100_000, message = "authors must be at most 100000")
        int authors,

        @Min(value = 1, message = "books must be at least 1")
        @Max(value = 200_000, message = "books must be at most 200000")
        int books,

        @Min(value = 0, message = "reviewsPerBook must not be negative")
        @Max(value = 500, message = "reviewsPerBook must be at most 500")
        int reviewsPerBook,

        @Min(value = 0, message = "authorsPerBook must not be negative")
        @Max(value = 20, message = "authorsPerBook must be at most 20")
        int authorsPerBook,

        @DecimalMin(value = "0.0", message = "coverRatio must not be negative")
        @DecimalMax(value = "1.0", message = "coverRatio must be at most 1.0")
        double coverRatio) {}
