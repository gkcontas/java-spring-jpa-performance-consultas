package com.gkcontas.queryperf;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SecondLevelCacheScenarioTest extends ScenarioTestBase {

    @Override
    protected String scenario() {
        return "second-level-cache";
    }

    @Test
    void withoutAnyCacheTheStatementIsSentEveryTime() {
        assertThat(of("no-cache").statements()).isEqualTo(1);
    }

    @Test
    void theEntityCacheAnswersFindByIdWithoutTouchingTheDatabase() {
        var fix = of("entity-cache-by-id");
        assertThat(fix.statements()).isZero();
        assertThat(fix.secondLevelHits()).isEqualTo(TEST_SHAPE.publishers());
    }

    /** The same cached entity, read by a query: the entity cache has nothing to offer. */
    @Test
    void theEntityCacheDoesNothingForAQuery() {
        var trap = of("entity-cache-by-query");
        assertThat(trap.statements()).isEqualTo(1);
        assertThat(trap.secondLevelHits()).isZero();
    }

    /**
     * The folklore says a cached query over an uncached entity degrades into one
     * statement per row. On Hibernate 6 it does not: no statement at all.
     */
    @Test
    void theQueryCacheWorksEvenWhenTheEntityIsNotCached() {
        var fix = of("query-cache");
        assertThat(fix.statements()).isZero();
        assertThat(fix.queryCacheHits()).isEqualTo(1);
    }

    @Test
    void aCachedQueryOverACachedEntityIsAlsoFree() {
        assertThat(of("query-cache-over-cached-entity").statements()).isZero();
        assertThat(of("query-cache-over-cached-entity").queryCacheHits()).isEqualTo(1);
    }

    /** One write anywhere in the table throws away every cached query over it. */
    @Test
    void oneWriteInvalidatesTheWholeTable() {
        var trap = of("query-cache-after-write");
        assertThat(trap.queryCacheMisses()).isEqualTo(1);
        assertThat(trap.queryCacheHits()).isZero();
        assertThat(trap.statements()).isGreaterThan(of("query-cache").statements());
    }
}
