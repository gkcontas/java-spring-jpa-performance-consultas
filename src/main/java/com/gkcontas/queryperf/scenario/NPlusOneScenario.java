package com.gkcontas.queryperf.scenario;

import com.gkcontas.queryperf.catalog.BatchedBook;
import com.gkcontas.queryperf.catalog.repository.BatchedBookRepository;
import com.gkcontas.queryperf.catalog.repository.BookRepository;
import com.gkcontas.queryperf.measure.ProbeOptions;
import com.gkcontas.queryperf.measure.QueryProbe;
import org.springframework.stereotype.Component;

/**
 * The N+1 itself, and the four repairs for it measured against each other.
 *
 * <p>The question is the smallest one that still needs the collection: how many reviews
 * each book has. No variant may answer it with fewer books than the others.
 */
@Component
public class NPlusOneScenario extends AbstractScenario {

    private final BookRepository books;
    private final BatchedBookRepository batchedBooks;

    public NPlusOneScenario(QueryProbe probe,
                            BookRepository books,
                            BatchedBookRepository batchedBooks) {
        super(probe);
        this.books = books;
        this.batchedBooks = batchedBooks;
    }

    @Override
    public String name() {
        return "n-plus-one";
    }

    @Override
    public String question() {
        return "For every book in the catalogue, how many reviews does it have?";
    }

    @Override
    protected String verdict() {
        return """
                Four repairs, three shapes. A fetch join and an entity graph produce the \
                same single statement and the same duplicated parent columns; batch \
                fetching keeps the parent query untouched and costs one statement per \
                batch; subselect fetching costs exactly two and re-runs the original \
                query as a subquery, which is cheap here and ruinous on a paginated \
                query. The one that does nothing is Session#setFetchBatchSize, which \
                reads like the runtime form of @BatchSize and is not: the collection \
                loader is built at startup from the annotation, so a batch size set on \
                the session arrives too late.""";
    }

    @Override
    protected void variants(Runner runner, ProbeOptions options) {
        runner.problem("lazy-loop",
                "One query for the books, then one more per book as the loop touches the collection",
                options,
                session -> {
                    var all = books.findAllPlain();
                    all.forEach(book -> book.getReviews().size());
                    return all;
                });

        runner.fix("entity-graph",
                "@EntityGraph(attributePaths = \"reviews\"), which Hibernate renders as a left join",
                options,
                session -> books.findAllWithReviewsGraph());

        runner.fix("join-fetch",
                "The same left join written out in the query as left join fetch",
                options,
                session -> books.findAllJoinFetchReviews());

        runner.fix("batch-size",
                "@BatchSize(" + BatchedBook.BATCH_SIZE
                        + ") on the collection: the parent query is untouched and the "
                        + "collections come back in batches",
                options,
                session -> {
                    var all = batchedBooks.findAllPlain();
                    all.forEach(book -> book.getReviews().size());
                    return all;
                });

        runner.fix("subselect",
                "Session#setSubselectFetchingEnabled: one statement for the books and one "
                        + "that re-runs their query as a subquery",
                options,
                session -> {
                    session.setSubselectFetchingEnabled(true);
                    var all = books.findAllPlain();
                    all.forEach(book -> book.getReviews().size());
                    return all;
                });

        runner.trap("session-fetch-batch-size",
                "Session#setFetchBatchSize(" + BatchedBook.BATCH_SIZE
                        + "), which looks like batch fetching at runtime and changes nothing",
                options,
                session -> {
                    session.setFetchBatchSize(BatchedBook.BATCH_SIZE);
                    var all = books.findAllPlain();
                    all.forEach(book -> book.getReviews().size());
                    return all;
                });
    }
}
