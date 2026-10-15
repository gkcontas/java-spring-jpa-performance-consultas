package com.gkcontas.queryperf;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CartesianProductScenarioTest extends ScenarioTestBase {

    @Override
    protected String scenario() {
        return "cartesian-product";
    }

    @Test
    void twoBagsDoNotRunAtAll() {
        var problem = of("two-bags");
        assertThat(problem.failure()).contains("MultipleBagFetchException");
        assertThat(problem.statements()).isZero();
    }

    /** Authors times reviews, which is what mapping both as Set leaves behind. */
    @Test
    void oneJoinCarriesTheProduct() {
        assertThat(of("one-join-rows").resultSize())
                .isEqualTo(TEST_SHAPE.authorsPerBook() * TEST_SHAPE.reviewsPerBook());
    }

    /** Authors plus reviews, in one statement each. */
    @Test
    void twoQueriesCarryTheSum() {
        var fix = of("two-queries");
        assertThat(fix.statements()).isEqualTo(2);
        assertThat(fix.resultSize())
                .isEqualTo(TEST_SHAPE.authorsPerBook() + TEST_SHAPE.reviewsPerBook());
    }

    @Test
    void theSumIsSmallerThanTheProduct() {
        assertThat(of("two-queries").resultSize())
                .isLessThan(of("one-join-rows").resultSize());
    }
}
