package com.gkcontas.queryperf.scenario;

import com.gkcontas.queryperf.catalog.BatchedBook;
import com.gkcontas.queryperf.catalog.repository.BatchedBookRepository;
import com.gkcontas.queryperf.catalog.repository.BookRepository;
import com.gkcontas.queryperf.measure.ProbeOptions;
import com.gkcontas.queryperf.measure.QueryProbe;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * A page of an aggregate with its collection, which is where the usual N+1 advice turns
 * into the worst plan in the project.
 *
 * <p>A fetch join makes one row per review, so the rows of the result set are not the
 * rows of the page. Hibernate cannot push {@code limit} into that query without
 * truncating collections, so it does not: it loads the whole query and keeps the first
 * page in memory, with one warning in the log - {@code HHH90003004} - and a statement
 * count of one. The statement count is why this survives review.
 */
@Component
public class PaginatedFetchJoinScenario extends AbstractScenario {

    static final int PAGE = 20;

    private final BookRepository books;
    private final BatchedBookRepository batchedBooks;

    public PaginatedFetchJoinScenario(QueryProbe probe,
                                      BookRepository books,
                                      BatchedBookRepository batchedBooks) {
        super(probe);
        this.books = books;
        this.batchedBooks = batchedBooks;
    }

    @Override
    public String name() {
        return "paginated-fetch-join";
    }

    @Override
    public String question() {
        return "The first page of " + PAGE + " books, each with its reviews.";
    }

    @Override
    protected String verdict() {
        return """
                Counting statements is not enough here, and this is the scenario that \
                proves it: the worst variant is the one with the fewest statements. A \
                fetch join on a paginated query is applied in memory, so it materialises \
                every book and every review in the catalogue to hand back twenty - one \
                statement, the whole table. The entity count is the number that gives it \
                away. Subselect fetching is worse again, because the subquery it \
                re-executes is the original query without the limit: it fetches the \
                collections of the entire table and then has to load the parents it \
                never selected. What works is either two statements - the page of ids, \
                then those ids with their collection - or leaving the page alone and \
                batching the collections.""";
    }

    @Override
    protected void variants(Runner runner, ProbeOptions options) {
        Pageable page = PageRequest.of(0, PAGE);

        runner.trap("fetch-join-page",
                "left join fetch on a paginated query: one statement, and "
                        + "firstResult/maxResults applied in memory over the whole catalogue",
                options,
                session -> books.findPageJoinFetchReviews(page).getContent());

        runner.fix("ids-then-fetch",
                "Two statements: the page of ids, which a limit can be applied to, then "
                        + "those ids with the collection joined",
                options,
                session -> {
                    List<Long> ids = books.findIdPage(page);
                    return books.findWithReviewsByIds(ids);
                });

        runner.fix("batch-size-page",
                "The page left as a plain query and the collections batched by @BatchSize("
                        + BatchedBook.BATCH_SIZE + ")",
                options,
                session -> {
                    var found = batchedBooks.findPagePlain(page);
                    found.forEach(book -> book.getReviews().size());
                    return found.getContent();
                });

        runner.trap("subselect-page",
                "Subselect fetching on a paginated query: the subquery is the original "
                        + "query without its limit",
                options,
                session -> {
                    session.setSubselectFetchingEnabled(true);
                    var found = books.findPagePlain(page);
                    found.forEach(book -> book.getReviews().size());
                    return found.getContent();
                });
    }
}
