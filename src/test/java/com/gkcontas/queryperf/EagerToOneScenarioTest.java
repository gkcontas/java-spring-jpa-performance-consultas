package com.gkcontas.queryperf;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EagerToOneScenarioTest extends ScenarioTestBase {

    @Override
    protected String scenario() {
        return "eager-to-one";
    }

    @Test
    void theDefaultFetchTypeFetchesTheBookNobodyRead() {
        assertThat(of("jpa-default-eager").statements()).isEqualTo(books() + 1);
    }

    @Test
    void oneWordOnTheMappingRemovesEveryExtraStatement() {
        assertThat(of("declared-lazy").statements()).isEqualTo(1);
    }

    @Test
    void joinFetchIsTheAnswerWhenTheBookIsActuallyNeeded() {
        assertThat(of("join-fetch").statements()).isEqualTo(1);
        assertThat(of("join-fetch").entitiesLoaded())
                .isGreaterThan(of("declared-lazy").entitiesLoaded());
    }

    @Test
    void allThreeReturnTheSamePage() {
        assertThat(report.variants()).allSatisfy(measurement ->
                assertThat(measurement.resultSize()).isEqualTo(200));
    }
}
