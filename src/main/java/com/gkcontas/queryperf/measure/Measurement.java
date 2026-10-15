package com.gkcontas.queryperf.measure;

import java.util.List;

/**
 * What one variant of one scenario cost.
 *
 * @param variant the name of the variant, e.g. {@code lazy-loop} or {@code entity-graph}
 * @param kind whether this variant is the problem or one of the repairs
 * @param explanation one line, in English, on what this variant does differently
 * @param statements JDBC statements prepared - the headline number of the project
 * @param entitiesLoaded entity instances materialised from a result set
 * @param collectionsLoaded lazy collections initialised
 * @param secondLevelHits second-level cache hits
 * @param secondLevelMisses second-level cache misses
 * @param queryCacheHits query cache hits
 * @param queryCacheMisses query cache misses
 * @param resultSize how many objects the unit of work handed back, so a cheaper
 *     variant cannot look cheap by answering a smaller question
 * @param millis median wall clock of the timed repetitions
 * @param failure the exception a variant died of, or {@code null}. Some of the
 *     variants compared here do not run at all, and "does not run" is a result
 * @param sql the distinct statements, collapsed, in the order they first appeared
 */
public record Measurement(
        String variant,
        VariantKind kind,
        String explanation,
        long statements,
        long entitiesLoaded,
        long collectionsLoaded,
        long secondLevelHits,
        long secondLevelMisses,
        long queryCacheHits,
        long queryCacheMisses,
        int resultSize,
        long millis,
        String failure,
        List<SqlStatement> sql) {}
