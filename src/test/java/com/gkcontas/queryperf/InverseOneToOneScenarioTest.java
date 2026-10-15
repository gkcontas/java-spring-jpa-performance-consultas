package com.gkcontas.queryperf;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InverseOneToOneScenarioTest extends ScenarioTestBase {

    @Override
    protected String scenario() {
        return "inverse-one-to-one";
    }

    /** Annotated LAZY, nothing read, one statement per row. */
    @Test
    void theInverseSideIsNotLazy() {
        assertThat(of("inverse-lazy").statements()).isEqualTo(books() + 1);
        assertThat(of("inverse-lazy").collectionsLoaded()).isZero();
    }

    @Test
    void fetchingItOnPurposeCostsOneLeftJoin() {
        assertThat(of("entity-graph").statements()).isEqualTo(1);
        assertThat(of("entity-graph").sql().getFirst().sql()).contains("left join book_cover");
    }

    @Test
    void notMappingTheInverseSideCostsNothingAtAll() {
        assertThat(of("no-inverse-side").statements()).isEqualTo(1);
        assertThat(of("no-inverse-side").sql().getFirst().sql()).doesNotContain("book_cover");
    }
}
