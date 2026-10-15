package com.gkcontas.queryperf.projection;

/**
 * Closed interface projection: every accessor maps to a column or to a path Spring Data
 * can resolve, so it can build a select of exactly these columns and never materialise
 * a {@code Book}.
 *
 * <p>The nested {@code getPublisher()} is the part worth noticing - a closed projection
 * can reach across an association and still stay closed, because the path is known at
 * query time.
 */
public interface BookSummary {

    String getTitle();

    int getPublishedYear();

    PublisherName getPublisher();

    interface PublisherName {
        String getName();
    }
}
