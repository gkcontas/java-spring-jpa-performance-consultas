package com.gkcontas.queryperf;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NativeSqlScenarioTest extends ScenarioTestBase {

    @Override
    protected String scenario() {
        return "native-sql";
    }

    @Test
    void bothReachOneRowPerBookInOneStatement() {
        assertThat(of("hql-window").statements()).isEqualTo(1);
        assertThat(of("native-distinct-on").statements()).isEqualTo(1);
        assertThat(of("hql-window").resultSize()).isEqualTo(books());
        assertThat(of("native-distinct-on").resultSize()).isEqualTo(books());
    }

    /** DISTINCT ON has no HQL spelling, which is the only argument for writing the SQL. */
    @Test
    void onlyTheNativeOneUsesTheDialectConstruct() {
        assertThat(of("native-distinct-on").sql().getFirst().sql()).contains("DISTINCT ON");
        assertThat(of("hql-window").sql().getFirst().sql()).contains("row_number() over");
    }

    @Test
    void neitherMaterialisesAnEntity() {
        assertThat(report.variants()).allSatisfy(measurement ->
                assertThat(measurement.entitiesLoaded()).isZero());
    }
}
