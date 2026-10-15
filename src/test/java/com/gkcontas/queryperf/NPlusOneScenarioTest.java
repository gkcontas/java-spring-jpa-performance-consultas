package com.gkcontas.queryperf;

import static org.assertj.core.api.Assertions.assertThat;

import com.gkcontas.queryperf.catalog.BatchedBook;
import com.gkcontas.queryperf.measure.VariantKind;
import org.junit.jupiter.api.Test;

class NPlusOneScenarioTest extends ScenarioTestBase {

    @Override
    protected String scenario() {
        return "n-plus-one";
    }

    @Test
    void theLoopCostsOneStatementPerBook() {
        assertThat(of("lazy-loop").statements()).isEqualTo(books() + 1);
        assertThat(of("lazy-loop").collectionsLoaded()).isEqualTo(books());
    }

    @Test
    void entityGraphAndJoinFetchBothCollapseToOneStatement() {
        assertThat(of("entity-graph").statements()).isEqualTo(1);
        assertThat(of("join-fetch").statements()).isEqualTo(1);
    }

    @Test
    void entityGraphAndJoinFetchProduceTheSameSql() {
        assertThat(of("entity-graph").sql().getFirst().sql())
                .isEqualTo(of("join-fetch").sql().getFirst().sql());
    }

    @Test
    void batchFetchingCostsOneStatementPerBatch() {
        int batches = (int) Math.ceil((double) books() / BatchedBook.BATCH_SIZE);
        assertThat(of("batch-size").statements()).isEqualTo(1 + batches);
    }

    @Test
    void subselectFetchingCostsExactlyTwo() {
        assertThat(of("subselect").statements()).isEqualTo(2);
        assertThat(of("subselect").sql().getLast().sql()).contains("in (select");
    }

    /**
     * The whole reason this variant exists. If a Hibernate upgrade ever makes
     * {@code setFetchBatchSize} work, this test fails and the README stops claiming it
     * does not.
     */
    @Test
    void sessionFetchBatchSizeChangesNothing() {
        assertThat(of("session-fetch-batch-size").kind()).isEqualTo(VariantKind.TRAP);
        assertThat(of("session-fetch-batch-size").statements())
                .isEqualTo(of("lazy-loop").statements());
    }

    @Test
    void everyVariantAnswersForEveryBook() {
        assertThat(report.variants()).allSatisfy(measurement ->
                assertThat(measurement.resultSize()).isEqualTo(books()));
    }
}
