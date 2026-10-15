package com.gkcontas.queryperf.scenario;

import com.gkcontas.queryperf.catalog.repository.BookRepository;
import com.gkcontas.queryperf.measure.ProbeOptions;
import com.gkcontas.queryperf.measure.QueryProbe;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Two collections of one aggregate, fetched at once.
 *
 * <p>Hibernate refuses with {@code MultipleBagFetchException}, and the advice the
 * exception message leads to - map both as {@code Set} instead of {@code List} - makes
 * the exception go away without making the problem go away. The reason the exception
 * exists is the join itself: one row per combination of author and review.
 */
@Component
public class CartesianProductScenario extends AbstractScenario {

    static final long BOOK_ID = 1L;

    private final BookRepository books;

    public CartesianProductScenario(QueryProbe probe, BookRepository books) {
        super(probe);
        this.books = books;
    }

    @Override
    public String name() {
        return "cartesian-product";
    }

    @Override
    public String question() {
        return "Book " + BOOK_ID + " with its authors and its reviews.";
    }

    @Override
    protected String verdict() {
        return """
                MultipleBagFetchException is not Hibernate being fussy about the \
                difference between a List and a Set. It is Hibernate refusing to \
                de-duplicate two collections out of one result set in which every row \
                is a pair. Turning both into a Set removes the refusal and leaves the \
                product: the authors are repeated once per review and the reviews once \
                per author, and the database carries all of it. Two statements carry \
                authors plus reviews instead of authors times reviews, and they are the \
                answer whatever the collection type is.""";
    }

    @Override
    protected void variants(Runner runner, ProbeOptions options) {
        runner.problem("two-bags",
                "join fetch on both collections at once. Does not run at all",
                ProbeOptions.single(),
                session -> books.findWithAuthorsAndReviews(BOOK_ID));

        runner.trap("one-join-rows",
                "What a double fetch join would have to carry: one row per pair of "
                        + "author and review, which is what mapping both as Set leaves behind",
                options,
                session -> books.findJoinRows(BOOK_ID));

        runner.fix("two-queries",
                "One statement per collection: authors plus reviews instead of "
                        + "authors times reviews",
                options,
                session -> {
                    List<Object> both = new ArrayList<>();
                    books.findWithAuthors(BOOK_ID)
                            .forEach(book -> both.addAll(book.getAuthors()));
                    books.findWithReviews(BOOK_ID)
                            .forEach(book -> both.addAll(book.getReviews()));
                    return both;
                });
    }
}
