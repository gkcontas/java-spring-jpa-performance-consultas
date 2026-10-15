package com.gkcontas.queryperf.scenario;

import com.gkcontas.queryperf.measure.ProbeOptions;
import com.gkcontas.queryperf.measure.QueryProbe;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Component;

/**
 * Whether dropping to native SQL pays, asked of the best case for it.
 *
 * <p>One row per group. HQL reaches it with a window function and a derived table.
 * PostgreSQL has {@code DISTINCT ON}, which is in no standard, has no HQL spelling, and
 * is the construct the dialect provides for exactly this shape - so if native SQL ever
 * wins, it wins here.
 */
@Component
public class NativeSqlScenario extends AbstractScenario {

    public NativeSqlScenario(QueryProbe probe) {
        super(probe);
    }

    @Override
    public String name() {
        return "native-sql";
    }

    @Override
    public String question() {
        return "The newest review of every book - one row per book.";
    }

    @Override
    protected String verdict() {
        return """
                It does not pay, and that is the measurement rather than an opinion. \
                Both variants are one statement, both return one row per book, and on \
                sixty thousand reviews they run in the same time: PostgreSQL sorts once \
                either way, and the window function over the sorted rows costs nothing \
                worth naming. The construct was chosen because it is the most favourable \
                case there is - a dialect-specific operator written for exactly this \
                shape, against HQL that has to spell it out in two layers - and it still \
                does not win. The point is not that native SQL never pays. It is that \
                "this is complicated, it has to be native" is not a reason, and the \
                scenarios that did produce an order-of-magnitude difference in this \
                project all did it by changing the number of round trips, not the \
                language the query was written in.""";
    }

    @Override
    protected void variants(Runner runner, ProbeOptions options) {
        runner.fix("hql-window",
                "Window function plus derived table in HQL, portable across dialects",
                options,
                session -> session.createQuery("""
                                select t.bookId, t.reviewId, t.createdAt from (
                                    select r.book.id as bookId,
                                           r.id as reviewId,
                                           r.createdAt as createdAt,
                                           row_number() over (
                                               partition by r.book.id
                                               order by r.createdAt desc, r.id desc) as rn
                                    from Review r
                                ) t
                                where t.rn = 1
                                order by t.bookId
                                """, Tuple.class)
                        .getResultList());

        runner.fix("native-distinct-on",
                "PostgreSQL DISTINCT ON: no HQL spelling, one sort instead of a window "
                        + "over the sorted rows",
                options,
                session -> session.createNativeQuery("""
                                SELECT DISTINCT ON (book_id) book_id, id, created_at
                                FROM review
                                ORDER BY book_id, created_at DESC, id DESC
                                """, Tuple.class)
                        .getResultList());
    }
}
