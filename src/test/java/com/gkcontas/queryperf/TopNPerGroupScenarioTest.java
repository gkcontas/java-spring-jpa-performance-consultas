package com.gkcontas.queryperf;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TopNPerGroupScenarioTest extends ScenarioTestBase {

    private static final int TOP = 3;

    @Override
    protected String scenario() {
        return "top-n-per-group";
    }

    @Test
    void doingItInJavaIsAnNPlusOneAndWastesEveryReview() {
        var problem = of("in-java");
        assertThat(problem.statements()).isEqualTo(books() + 1);
        assertThat(problem.entitiesLoaded()).isEqualTo(books() + reviews());
    }

    /** The same round trips with none of the wasted entities. */
    @Test
    void oneQueryPerBookWastesNothingAndStillCostsOnePerBook() {
        var problem = of("jpql-per-book");
        assertThat(problem.statements()).isEqualTo(books() + 1);
        assertThat(problem.entitiesLoaded()).isZero();
    }

    /** HQL 6 has window functions and derived tables; this is the finding. */
    @Test
    void hqlAloneGetsItDoneInOneStatement() {
        var fix = of("hql-window");
        assertThat(fix.statements()).isEqualTo(1);
        assertThat(fix.sql().getFirst().sql()).contains("row_number() over");
    }

    @Test
    void theNativeVersionBuysNoFewerStatements() {
        assertThat(of("native-window").statements()).isEqualTo(of("hql-window").statements());
    }

    @Test
    void everyVariantReturnsTheSameRanking() {
        assertThat(report.variants()).allSatisfy(measurement ->
                assertThat(measurement.resultSize()).isEqualTo(books() * TOP));
    }
}
