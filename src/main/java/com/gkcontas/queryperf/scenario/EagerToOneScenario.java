package com.gkcontas.queryperf.scenario;

import com.gkcontas.queryperf.catalog.repository.LeanReviewRepository;
import com.gkcontas.queryperf.catalog.repository.ReviewRepository;
import com.gkcontas.queryperf.measure.ProbeOptions;
import com.gkcontas.queryperf.measure.QueryProbe;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * The N+1 nobody wrote: {@code @ManyToOne} and {@code @OneToOne} are {@code EAGER} by
 * default in JPA, and an eager to-one in an HQL query is not turned into a join - it is
 * resolved row by row after the result set comes back.
 *
 * <p>The page is ordered by reviewer and not by the foreign key, which is the only
 * honest way to measure it. Ordered by {@code book_id} the first row of each book warms
 * the session for the rest, so a page of two hundred reviews touches a handful of books
 * and the eager fetch looks nearly free.
 */
@Component
public class EagerToOneScenario extends AbstractScenario {

    static final int PAGE = 200;

    private final ReviewRepository reviews;
    private final LeanReviewRepository leanReviews;

    public EagerToOneScenario(QueryProbe probe,
                              ReviewRepository reviews,
                              LeanReviewRepository leanReviews) {
        super(probe);
        this.reviews = reviews;
        this.leanReviews = leanReviews;
    }

    @Override
    public String name() {
        return "eager-to-one";
    }

    @Override
    public String question() {
        return "A page of " + PAGE + " reviews, ordered by reviewer. The book is not read.";
    }

    @Override
    protected String verdict() {
        return """
                The two mappings differ by one word and the same page costs a statement \
                against two hundred. Nothing in the code touches getBook(): the fetch \
                type decided it, and the default fetch type of a to-one is EAGER, so \
                this is the one N+1 that arrives without anybody writing a loop. When \
                the book really is needed, a join fetch brings it in the original \
                statement - the choice is between one statement and many, never between \
                eager and correct.""";
    }

    @Override
    protected void variants(Runner runner, ProbeOptions options) {
        runner.problem("jpa-default-eager",
                "Review.book left on the JPA default, which is EAGER: one statement per "
                        + "distinct book in the page",
                options,
                session -> reviews.findPageSpread(PageRequest.of(0, PAGE)));

        runner.fix("declared-lazy",
                "The same table mapped with fetch = LAZY: the book is never fetched "
                        + "because it is never read",
                options,
                session -> leanReviews.findPageSpread(PageRequest.of(0, PAGE)));

        runner.fix("join-fetch",
                "For when the book is needed: join fetch brings it in the original statement",
                options,
                session -> reviews.findPageSpreadJoinFetch(PageRequest.of(0, PAGE)));
    }
}
