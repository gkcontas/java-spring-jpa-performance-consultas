package com.gkcontas.queryperf.projection;

/**
 * DTO projection, built by a constructor expression in JPQL. The columns are written
 * out in the query, so there is nothing for Spring Data to infer and nothing that can
 * silently widen.
 */
public record BookSummaryDto(String title, int publishedYear, String publisherName) {}
