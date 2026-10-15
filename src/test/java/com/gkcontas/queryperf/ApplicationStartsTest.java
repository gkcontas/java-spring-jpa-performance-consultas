package com.gkcontas.queryperf;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApplicationStartsTest extends IntegrationTestBase {

    @Test
    void migrationsCreateTheCatalogue() {
        assertThat(count("SELECT count(*) FROM information_schema.tables "
                + "WHERE table_schema = 'public' AND table_name IN "
                + "('publisher', 'author', 'book', 'book_author', 'review', 'book_cover')"))
                .isEqualTo(6);
    }

    @Test
    void seedingFillsEveryTable() {
        seedOnce();
        assertThat(count("SELECT count(*) FROM book")).isEqualTo(TEST_SHAPE.books());
        assertThat(count("SELECT count(*) FROM review"))
                .isEqualTo((long) TEST_SHAPE.books() * TEST_SHAPE.reviewsPerBook());
        assertThat(count("SELECT count(*) FROM book_author"))
                .isEqualTo((long) TEST_SHAPE.books() * TEST_SHAPE.authorsPerBook());
        // coverRatio 0.5 means every second book, which is what the seeder writes.
        assertThat(count("SELECT count(*) FROM book_cover"))
                .isEqualTo(Math.round(TEST_SHAPE.books() * TEST_SHAPE.coverRatio()));
    }
}
