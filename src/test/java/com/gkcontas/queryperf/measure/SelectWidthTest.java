package com.gkcontas.queryperf.measure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * The select width is what settles the projection argument, so it is worth being sure
 * it counts the right commas. Hibernate's generated SQL is the only input it has to
 * handle, which is why the cases here are copied from statements it produced.
 */
class SelectWidthTest {

    @Test
    void countsAPlainSelectList() {
        assertThat(SelectWidth.of("select b1_0.title,b1_0.published_year from book b1_0"))
                .isEqualTo(2);
    }

    @Test
    void ignoresCommasInsideFunctionCalls() {
        assertThat(SelectWidth.of(
                "select coalesce(a,b),count(*) from review r1_0"))
                .isEqualTo(2);
    }

    @Test
    void ignoresTheFromOfASubquery() {
        assertThat(SelectWidth.of(
                "select r1_0.id from review r1_0 where r1_0.book_id in "
                        + "(select b1_0.id from book b1_0)"))
                .isEqualTo(1);
    }

    @Test
    void countsAWindowFunctionAsOneColumn() {
        assertThat(SelectWidth.of(
                "select r1_0.book_id,row_number() over(partition by r1_0.book_id "
                        + "order by r1_0.rating desc) from review r1_0"))
                .isEqualTo(2);
    }

    /** A statement that is not a select has no select list, and reports zero. */
    @Test
    void reportsZeroForAnUpdate() {
        assertThat(SelectWidth.of("update book set title=? where id=?")).isZero();
    }

    @Test
    void reportsZeroWhenThereIsNoFromAtAll() {
        assertThat(SelectWidth.of("select setval(?,?)")).isZero();
    }
}
