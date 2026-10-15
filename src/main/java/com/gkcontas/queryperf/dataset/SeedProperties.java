package com.gkcontas.queryperf.dataset;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Shape of the seeded catalogue.
 *
 * <p>The defaults are chosen so an N+1 is visible as a duration and not only as a
 * counter: two thousand books is enough that two thousand round trips cost real
 * milliseconds on a container on the same machine, and still small enough that the
 * whole set seeds in a couple of seconds.
 */
@ConfigurationProperties(prefix = "app.seed")
public record SeedProperties(
        int publishers,
        int authors,
        int books,
        int reviewsPerBook,
        int authorsPerBook,
        double coverRatio,
        int batchSize) {}
