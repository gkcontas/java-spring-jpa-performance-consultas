package com.gkcontas.queryperf.scenario;

import com.gkcontas.queryperf.catalog.Publisher;
import com.gkcontas.queryperf.catalog.repository.BookRepository;
import com.gkcontas.queryperf.catalog.repository.PublisherRepository;
import com.gkcontas.queryperf.dataset.SeedProperties;
import com.gkcontas.queryperf.measure.ProbeOptions;
import com.gkcontas.queryperf.measure.QueryProbe;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * What each level of cache is actually for, measured on the second read.
 *
 * <p>Every variant here runs with the caches kept rather than emptied, and with one
 * warm-up whose cost is discarded - because a cache that is measured cold is not being
 * measured at all.
 */
@Component
public class SecondLevelCacheScenario extends AbstractScenario {

    private static final int YEAR = 2000;
    private static final String COUNTRY = "Brazil";

    private final PublisherRepository publishers;
    private final BookRepository books;
    private final SeedProperties seed;

    public SecondLevelCacheScenario(QueryProbe probe,
                                    PublisherRepository publishers,
                                    BookRepository books,
                                    SeedProperties seed) {
        super(probe);
        this.publishers = publishers;
        this.books = books;
        this.seed = seed;
    }

    @Override
    public String name() {
        return "second-level-cache";
    }

    @Override
    public String question() {
        return "Reference data and a filtered list, read a second time in a new session.";
    }

    @Override
    protected String verdict() {
        return """
                The entity cache answers find-by-id and nothing else: it turns the \
                second read of a publisher into a cache hit and leaves every query over \
                publishers exactly as expensive as it was. The query cache is what \
                answers a query, and the usual warning about it - that over an entity \
                which is not itself cached it degrades into one statement per row - does \
                not hold on Hibernate 6: the cached result comes back whole, with no \
                statement at all. What does hold is the invalidation. The query cache is \
                keyed by the query and invalidated by the table, so one write anywhere in \
                book throws away every cached query over book - which is why the \
                candidates for it are the tables nobody writes to.""";
    }

    @Override
    protected void variants(Runner runner, ProbeOptions options) {
        ProbeOptions warm = options.keepingCaches();

        runner.problem("no-cache",
                "A query over an entity with no cache of any kind: the same statement "
                        + "on every read",
                warm,
                session -> books.findByYearUncached(YEAR));

        runner.fix("entity-cache-by-id",
                "@Cacheable on Publisher, read by id: answered entirely from the "
                        + "second-level cache",
                warm,
                session -> {
                    List<Publisher> found = new ArrayList<>();
                    for (long id = 1; id <= seed.publishers(); id++) {
                        found.add(session.find(Publisher.class, id));
                    }
                    return found;
                });

        runner.trap("entity-cache-by-query",
                "The same cached entity read by a query instead of by id: the entity "
                        + "cache has nothing to say about a query",
                warm,
                session -> publishers.findAll());

        runner.fix("query-cache",
                "org.hibernate.cacheable on a query over an entity that is NOT cached: "
                        + "the result comes back whole, with no statement",
                warm,
                session -> books.findByYearCached(YEAR));

        runner.fix("query-cache-over-cached-entity",
                "A cached query over a cached entity, which is the combination the "
                        + "documentation recommends",
                warm,
                session -> publishers.findByCountryCached(COUNTRY));

        runner.trap("query-cache-after-write",
                "One write anywhere in book, then the same cached query: the write "
                        + "invalidates every cached query over the table",
                warm,
                session -> {
                    session.createMutationQuery(
                                    "update Book b set b.title = b.title where b.id = 1")
                            .executeUpdate();
                    return books.findByYearCached(YEAR);
                });
    }
}
