package com.gkcontas.queryperf;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PaginatedFetchJoinScenarioTest extends ScenarioTestBase {

    private static final int PAGE = 20;

    @Override
    protected String scenario() {
        return "paginated-fetch-join";
    }

    /**
     * The scenario the statement count cannot see. Two statements, and the whole
     * catalogue materialised to hand back one page.
     */
    @Test
    void aFetchJoinOnAPagedQueryLoadsEverything() {
        var trap = of("fetch-join-page");
        assertThat(trap.statements()).isEqualTo(2);
        assertThat(trap.resultSize()).isEqualTo(PAGE);
        assertThat(trap.entitiesLoaded()).isEqualTo(books() + reviews());
        assertThat(trap.collectionsLoaded()).isEqualTo(books());
    }

    @Test
    void theTwoStatementPageLoadsOnlyThePage() {
        var fix = of("ids-then-fetch");
        assertThat(fix.statements()).isEqualTo(2);
        assertThat(fix.resultSize()).isEqualTo(PAGE);
        assertThat(fix.collectionsLoaded()).isEqualTo(PAGE);
    }

    /** Same statement count as the trap, a third of the entities. */
    @Test
    void sameStatementCountIsNotSameCost() {
        assertThat(of("ids-then-fetch").statements())
                .isEqualTo(of("fetch-join-page").statements());
        assertThat(of("ids-then-fetch").entitiesLoaded())
                .isLessThan(of("fetch-join-page").entitiesLoaded());
    }

    @Test
    void batchingTheCollectionsLeavesThePageAlone() {
        var fix = of("batch-size-page");
        assertThat(fix.statements()).isEqualTo(3);
        assertThat(fix.collectionsLoaded()).isEqualTo(PAGE);
    }

    /** The subquery is the original query stripped of its limit. */
    @Test
    void subselectFetchingIgnoresTheLimit() {
        var trap = of("subselect-page");
        assertThat(trap.collectionsLoaded()).isEqualTo(books());
        assertThat(trap.statements()).isGreaterThan(of("ids-then-fetch").statements());
        assertThat(trap.sql()).anySatisfy(statement ->
                assertThat(statement.sql()).contains("in (select").doesNotContain("fetch first"));
    }

    @Test
    void everyVariantReturnsThePage() {
        assertThat(report.variants()).allSatisfy(measurement ->
                assertThat(measurement.resultSize()).isEqualTo(PAGE));
    }
}
