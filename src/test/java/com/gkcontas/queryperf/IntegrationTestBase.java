package com.gkcontas.queryperf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkcontas.queryperf.dataset.DatasetSeeder;
import com.gkcontas.queryperf.dataset.SeedProperties;
import com.gkcontas.queryperf.measure.Measurement;
import com.gkcontas.queryperf.scenario.ScenarioReport;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * PostgreSQL, started once per JVM.
 *
 * <p>The database is real because the entire subject is what the database is asked. A
 * statement count against an in-memory stand-in would be a statement count against a
 * different optimiser, a different driver and a different notion of what a cursor is.
 *
 * <p>Started from a static initialiser and not through {@code @Testcontainers}: Spring
 * caches the application context across test classes, and the extension would stop the
 * container when the first class finished while a cached context still pointed at it.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTestBase {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("bookstore")
            .withUsername("bookstore")
            .withPassword("bookstore");

    static {
        POSTGRES.start();
    }

    /**
     * A small catalogue for the tests. They assert on exact statement counts, which are
     * a function of the number of rows, so the shape has to be fixed here and not
     * inherited from {@code application.yml}: 60 books, 4 reviews each, 2 authors each.
     *
     * <p>Sixty and not twenty, because the pagination scenario asks for a page of
     * twenty. With a catalogue of twenty the page is the whole catalogue, and the
     * variant that loads the whole catalogue to return a page would be indistinguishable
     * from the one that does not - the test would pass while measuring nothing.
     */
    protected static final SeedProperties TEST_SHAPE =
            new SeedProperties(4, 30, 60, 4, 2, 0.5, 200);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.seed.books", () -> TEST_SHAPE.books());
        registry.add("app.seed.publishers", () -> TEST_SHAPE.publishers());
        registry.add("app.seed.authors", () -> TEST_SHAPE.authors());
        registry.add("app.seed.reviews-per-book", () -> TEST_SHAPE.reviewsPerBook());
        registry.add("app.seed.authors-per-book", () -> TEST_SHAPE.authorsPerBook());
        registry.add("app.seed.cover-ratio", () -> TEST_SHAPE.coverRatio());
    }

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected DatasetSeeder seeder;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    private static boolean seeded;

    /**
     * Seeded once per test class and never mutated afterwards, apart from the one
     * cache variant that writes a book title back onto itself. Re-seeding per test
     * method would only add seconds to a catalogue that every scenario reads and none
     * of them changes.
     */
    @BeforeAll
    static void markUnseeded() {
        seeded = false;
    }

    protected void seedOnce() {
        if (!seeded) {
            reseed();
        }
    }

    /** Puts the catalogue back to the test shape, for the tests that change it. */
    protected void reseed() {
        seeder.seed(TEST_SHAPE);
        seeded = true;
    }

    protected long count(String sql) {
        Long value = jdbc.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }

    protected static Measurement variant(ScenarioReport report, String name) {
        return report.variants().stream()
                .filter(measurement -> measurement.variant().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "no variant '" + name + "' in " + names(report)));
    }

    protected static List<String> names(ScenarioReport report) {
        return report.variants().stream().map(Measurement::variant).toList();
    }
}
