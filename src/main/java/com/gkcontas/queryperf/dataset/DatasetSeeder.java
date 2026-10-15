package com.gkcontas.queryperf.dataset;

import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fills the catalogue through plain JDBC, in batches, with explicit primary keys.
 *
 * <p>Through JPA this would be a slow and irrelevant detour: the seeder is setup, not
 * subject matter, and persisting sixty thousand reviews one entity at a time would take
 * longer than every scenario put together.
 *
 * <p>Explicit keys rather than sequences, so ids are 1..n and a scenario can name the
 * row it wants. The sequences are moved past the seeded range afterwards, otherwise the
 * first entity written through JPA collides with a seeded id.
 *
 * <p>Everything comes from a {@link Random} with a fixed seed, so two runs of the same
 * configuration produce the same catalogue and two measurements are comparable.
 */
@Component
public class DatasetSeeder {

    private static final long SEED = 20261015L;

    private static final List<String> COUNTRIES =
            List.of("Brazil", "Portugal", "Argentina", "Spain", "France", "Japan");
    private static final List<String> FORMATS =
            List.of("HARDCOVER", "PAPERBACK", "EBOOK", "AUDIOBOOK");
    private static final List<String> COLORS =
            List.of("slate", "amber", "indigo", "crimson", "teal", "olive");
    private static final List<String> SUBJECTS =
            List.of("Transactions", "Indexes", "Caches", "Queues", "Replication",
                    "Partitions", "Locks", "Sessions", "Mappings", "Projections");
    private static final List<String> QUALIFIERS =
            List.of("in Practice", "from Scratch", "Reconsidered", "at Scale",
                    "for Sceptics", "and Their Discontents");

    private final JdbcTemplate jdbc;
    private final SeedProperties properties;

    public DatasetSeeder(JdbcTemplate jdbc, SeedProperties properties) {
        this.jdbc = jdbc;
        this.properties = properties;
    }

    @Transactional
    public SeedReport seed() {
        return seed(properties);
    }

    @Transactional
    public SeedReport seed(SeedProperties shape) {
        truncate();
        Random random = new Random(SEED);

        insertPublishers(shape);
        insertAuthors(shape);
        insertBooks(shape, random);
        int links = insertBookAuthors(shape, random);
        int reviews = insertReviews(shape, random);
        int covers = insertCovers(shape);
        realignSequences(shape);

        return new SeedReport(shape.publishers(), shape.authors(), shape.books(),
                links, reviews, covers);
    }

    @Transactional
    public void truncate() {
        jdbc.execute("""
                TRUNCATE book_cover, review, book_author, book, author, publisher
                RESTART IDENTITY CASCADE
                """);
    }

    private void insertPublishers(SeedProperties shape) {
        List<Object[]> rows = new ArrayList<>();
        for (int i = 1; i <= shape.publishers(); i++) {
            rows.add(new Object[] {
                    (long) i,
                    "Publisher " + (char) ('A' + (i - 1) % 26) + i,
                    COUNTRIES.get((i - 1) % COUNTRIES.size())
            });
        }
        batch("INSERT INTO publisher (id, name, country) VALUES (?, ?, ?)", rows,
                new int[] {Types.BIGINT, Types.VARCHAR, Types.VARCHAR});
    }

    private void insertAuthors(SeedProperties shape) {
        List<Object[]> rows = new ArrayList<>();
        for (int i = 1; i <= shape.authors(); i++) {
            rows.add(new Object[] {
                    (long) i,
                    "Author %04d".formatted(i),
                    COUNTRIES.get(i % COUNTRIES.size())
            });
        }
        batch("INSERT INTO author (id, name, nationality) VALUES (?, ?, ?)", rows,
                new int[] {Types.BIGINT, Types.VARCHAR, Types.VARCHAR});
    }

    private void insertBooks(SeedProperties shape, Random random) {
        List<Object[]> rows = new ArrayList<>();
        for (int i = 1; i <= shape.books(); i++) {
            String title = "%s %s".formatted(
                    SUBJECTS.get(random.nextInt(SUBJECTS.size())),
                    QUALIFIERS.get(random.nextInt(QUALIFIERS.size())));
            rows.add(new Object[] {
                    (long) i,
                    "%s, v%d".formatted(title, i),
                    "978-%010d".formatted(i),
                    1990 + random.nextInt(36),
                    java.math.BigDecimal.valueOf(1990 + random.nextInt(8010), 2),
                    FORMATS.get(random.nextInt(FORMATS.size())),
                    (long) (1 + (i - 1) % shape.publishers())
            });
        }
        batch("""
                INSERT INTO book (id, title, isbn, published_year, price, format, publisher_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, rows,
                new int[] {Types.BIGINT, Types.VARCHAR, Types.VARCHAR, Types.INTEGER,
                        Types.NUMERIC, Types.VARCHAR, Types.BIGINT});
    }

    private int insertBookAuthors(SeedProperties shape, Random random) {
        List<Object[]> rows = new ArrayList<>();
        for (int book = 1; book <= shape.books(); book++) {
            Set<Integer> picked = new LinkedHashSet<>();
            while (picked.size() < Math.min(shape.authorsPerBook(), shape.authors())) {
                picked.add(1 + random.nextInt(shape.authors()));
            }
            for (Integer author : picked) {
                rows.add(new Object[] {(long) book, (long) author});
            }
        }
        batch("INSERT INTO book_author (book_id, author_id) VALUES (?, ?)", rows,
                new int[] {Types.BIGINT, Types.BIGINT});
        return rows.size();
    }

    private int insertReviews(SeedProperties shape, Random random) {
        OffsetDateTime base = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        List<Object[]> rows = new ArrayList<>();
        long id = 0;
        for (int book = 1; book <= shape.books(); book++) {
            for (int n = 0; n < shape.reviewsPerBook(); n++) {
                rows.add(new Object[] {
                        ++id,
                        (long) book,
                        "Reader %05d".formatted(random.nextInt(50_000)),
                        1 + random.nextInt(5),
                        "Review body for book %d, note %d. ".formatted(book, n)
                                + "Long enough to be a text column and not a label.",
                        base.plusMinutes(id)
                });
            }
        }
        batch("""
                INSERT INTO review (id, book_id, reviewer_name, rating, body, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, rows,
                new int[] {Types.BIGINT, Types.BIGINT, Types.VARCHAR, Types.INTEGER,
                        Types.VARCHAR, Types.TIMESTAMP_WITH_TIMEZONE});
        return rows.size();
    }

    private int insertCovers(SeedProperties shape) {
        int every = shape.coverRatio() <= 0 ? Integer.MAX_VALUE
                : (int) Math.max(1, Math.round(1 / shape.coverRatio()));
        List<Object[]> rows = new ArrayList<>();
        for (int book = 1; book <= shape.books(); book++) {
            if (book % every != 0) {
                continue;
            }
            rows.add(new Object[] {
                    (long) book,
                    COLORS.get(book % COLORS.size()),
                    600 + (book % 5) * 40,
                    900 + (book % 7) * 40
            });
        }
        batch("""
                INSERT INTO book_cover (book_id, dominant_color, width, height)
                VALUES (?, ?, ?, ?)
                """, rows,
                new int[] {Types.BIGINT, Types.VARCHAR, Types.INTEGER, Types.INTEGER});
        return rows.size();
    }

    /**
     * The rows went in with explicit ids, which leaves every sequence at 1. Anything
     * written through JPA afterwards would be handed id 1 and collide.
     */
    private void realignSequences(SeedProperties shape) {
        setval("publisher_id_seq", shape.publishers());
        setval("author_id_seq", shape.authors());
        setval("book_id_seq", shape.books());
        setval("review_id_seq", (long) shape.books() * shape.reviewsPerBook());
    }

    private void setval(String sequence, long value) {
        jdbc.queryForObject("SELECT setval(?, ?)", Long.class, sequence, Math.max(1, value));
    }

    private void batch(String sql, List<Object[]> rows, int[] types) {
        for (int from = 0; from < rows.size(); from += properties.batchSize()) {
            int to = Math.min(rows.size(), from + properties.batchSize());
            jdbc.batchUpdate(sql, rows.subList(from, to), types);
        }
    }
}
