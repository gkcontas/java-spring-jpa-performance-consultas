package com.gkcontas.queryperf.scenario;

import com.gkcontas.queryperf.catalog.repository.BookRepository;
import com.gkcontas.queryperf.measure.ProbeOptions;
import com.gkcontas.queryperf.measure.QueryProbe;
import org.springframework.stereotype.Component;

/**
 * Five ways to ask for three columns, and the select list each one actually sends.
 *
 * <p>"Select only what you need" is advice everybody repeats and nobody checks. Two of
 * the five variants here look like projections, are declared like projections, and
 * select every column of the entity.
 */
@Component
public class ProjectionScenario extends AbstractScenario {

    /** Below every seeded year, so every variant answers for the whole catalogue. */
    private static final int ALL_YEARS = 0;

    private final BookRepository books;

    public ProjectionScenario(QueryProbe probe, BookRepository books) {
        super(probe);
        this.books = books;
    }

    @Override
    public String name() {
        return "projections";
    }

    @Override
    public String question() {
        return "Title, publication year and publisher name of every book.";
    }

    @Override
    protected String verdict() {
        return """
                Read the select width, not the statement count. Four of the five \
                variants send a single statement and they do not ask for the same thing, \
                and even the loop that goes back for each publisher is cheap in \
                statements, because an N+1 over a to-one repeats only once per distinct \
                value and there are eight publishers for two thousand books. \
                An open interface projection - one SpEL @Value is enough to make it open \
                - has to be handed the root object for SpEL to evaluate against, so \
                Spring Data selects the whole entity and materialises it. A closed \
                projection over an @Query that selects the entity is not a projection \
                either, because Spring Data only narrows a select it wrote itself. On a \
                derived method the closed projection does narrow, but a nested projection \
                widens to every column of the association, since what it fetches is the \
                associated entity. Only the constructor expression sends exactly three \
                columns and materialises nothing.""";
    }

    @Override
    protected void variants(Runner runner, ProbeOptions options) {
        runner.problem("entity-then-lazy",
                "The entities, then the publisher of each one through the lazy to-one. "
                        + "The statement count is low because an N+1 over a to-one is "
                        + "capped by the number of distinct values, not by the number of rows",
                options,
                session -> {
                    var all = books.findAllPlain();
                    all.forEach(book -> book.getPublisher().getName());
                    return all;
                });

        runner.trap("closed-over-entity-query",
                "A closed interface projection over @Query(\"select b from Book b\"): "
                        + "Spring Data narrows only the selects it writes itself",
                options,
                session -> books.findAllSummaryClosedOverEntityQuery());

        runner.trap("open-projection",
                "An interface projection with a SpEL @Value, which needs the root object "
                        + "and therefore the whole entity",
                options,
                session -> books.findByPublishedYearGreaterThanOrderByIdAsc(ALL_YEARS));

        runner.fix("closed-derived",
                "The same closed projection on a derived method, where the nested "
                        + "publisher widens the select to the whole association",
                options,
                session -> books.findByPublishedYearGreaterThanOrderById(ALL_YEARS));

        runner.fix("dto-constructor",
                "A constructor expression in JPQL: three columns named in the query and "
                        + "no entity materialised",
                options,
                session -> books.findAllSummaryDto());
    }
}
