package com.gkcontas.queryperf.scenario;

import com.gkcontas.queryperf.catalog.repository.BookRepository;
import com.gkcontas.queryperf.catalog.repository.CoveredBookRepository;
import com.gkcontas.queryperf.measure.ProbeOptions;
import com.gkcontas.queryperf.measure.QueryProbe;
import org.springframework.stereotype.Component;

/**
 * The association that is annotated lazy and is not.
 *
 * <p>On the inverse side of a one-to-one the foreign key is in the other table, so
 * nothing in the parent row says whether the child exists. Hibernate has to choose
 * between installing a proxy and writing {@code null}, and without bytecode enhancement
 * the only way to choose is to go and look. One statement per row, before anything is
 * read.
 */
@Component
public class InverseOneToOneScenario extends AbstractScenario {

    private final CoveredBookRepository coveredBooks;
    private final BookRepository books;

    public InverseOneToOneScenario(QueryProbe probe,
                                   CoveredBookRepository coveredBooks,
                                   BookRepository books) {
        super(probe);
        this.coveredBooks = coveredBooks;
        this.books = books;
    }

    @Override
    public String name() {
        return "inverse-one-to-one";
    }

    @Override
    public String question() {
        return "The title of every book. The cover is never read.";
    }

    @Override
    protected String verdict() {
        return """
                This is the only N+1 in the project that no query and no loop asks for. \
                The word LAZY is on the mapping, the cover is never read, and the \
                statement count is still one per book. An optional inverse one-to-one is \
                the one association where the fetch type is a request rather than an \
                instruction. The repairs are to fetch it on purpose with an entity graph, \
                or - better, when the parent does not need to navigate to the child - not \
                to map the inverse side at all and let the child point at the parent.""";
    }

    @Override
    protected void variants(Runner runner, ProbeOptions options) {
        runner.problem("inverse-lazy",
                "An optional @OneToOne(mappedBy = ..., fetch = LAZY): one statement per "
                        + "book to decide between a proxy and null",
                options,
                session -> coveredBooks.findAllPlain());

        runner.fix("entity-graph",
                "The same mapping with the cover fetched on purpose: one left join",
                options,
                session -> coveredBooks.findAllWithCoverGraph());

        runner.fix("no-inverse-side",
                "The mapping that does not have the problem: only the child points at "
                        + "the parent",
                options,
                session -> books.findAllPlain());
    }
}
