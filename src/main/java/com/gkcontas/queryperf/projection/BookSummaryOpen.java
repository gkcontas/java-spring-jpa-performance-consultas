package com.gkcontas.queryperf.projection;

import org.springframework.beans.factory.annotation.Value;

/**
 * Open interface projection. One SpEL {@code @Value} is enough to make it open, and an
 * open projection is not a projection at all: SpEL is evaluated against the root
 * object, so Spring Data has to hand it a whole {@code Book}, which means selecting
 * every column and materialising the entity.
 *
 * <p>It reads like the closed one, it is declared like the closed one, and the scenario
 * {@code projections} shows the select list going from three columns to all of them.
 */
public interface BookSummaryOpen {

    @Value("#{target.title}")
    String getTitle();

    @Value("#{target.publishedYear}")
    int getPublishedYear();

    @Value("#{target.publisher.name}")
    String getPublisherName();
}
