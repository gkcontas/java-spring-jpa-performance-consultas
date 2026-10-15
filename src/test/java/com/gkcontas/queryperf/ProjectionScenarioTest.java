package com.gkcontas.queryperf;

import static org.assertj.core.api.Assertions.assertThat;

import com.gkcontas.queryperf.measure.Measurement;
import org.junit.jupiter.api.Test;

class ProjectionScenarioTest extends ScenarioTestBase {

    /** Columns of the book table, which is what "no projection at all" looks like. */
    private static final int ALL_BOOK_COLUMNS = 7;

    @Override
    protected String scenario() {
        return "projections";
    }

    private int width(String variant) {
        Measurement measurement = of(variant);
        return measurement.sql().getFirst().selectedColumns();
    }

    @Test
    void aClosedProjectionOverAnEntityQueryIsNotAProjection() {
        assertThat(width("closed-over-entity-query")).isEqualTo(ALL_BOOK_COLUMNS);
        assertThat(of("closed-over-entity-query").entitiesLoaded()).isEqualTo(books());
    }

    @Test
    void anOpenProjectionIsNotAProjectionEither() {
        assertThat(width("open-projection")).isEqualTo(ALL_BOOK_COLUMNS);
        assertThat(of("open-projection").entitiesLoaded()).isEqualTo(books());
    }

    /** It narrows, and the nested projection widens it to the whole association. */
    @Test
    void aClosedProjectionOnADerivedMethodNarrowsTheSelect() {
        assertThat(width("closed-derived")).isLessThan(ALL_BOOK_COLUMNS);
        assertThat(of("closed-derived").entitiesLoaded())
                .isEqualTo(TEST_SHAPE.publishers());
    }

    @Test
    void onlyTheConstructorExpressionSelectsExactlyWhatWasAsked() {
        assertThat(width("dto-constructor")).isEqualTo(3);
        assertThat(of("dto-constructor").entitiesLoaded()).isZero();
    }

    /** A to-one N+1 is capped by distinct values, not by rows - eight, not two thousand. */
    @Test
    void theToOneLoopRepeatsOncePerDistinctPublisher() {
        assertThat(of("entity-then-lazy").statements())
                .isEqualTo(1 + TEST_SHAPE.publishers());
    }

    @Test
    void everyVariantAnswersForEveryBook() {
        assertThat(report.variants()).allSatisfy(measurement ->
                assertThat(measurement.resultSize()).isEqualTo(books()));
    }
}
