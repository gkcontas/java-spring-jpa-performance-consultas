package com.gkcontas.queryperf.scenario;

import com.gkcontas.queryperf.catalog.repository.BookRepository;
import com.gkcontas.queryperf.measure.ProbeOptions;
import com.gkcontas.queryperf.measure.QueryProbe;
import jakarta.persistence.Tuple;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Top N per group: the query every reporting screen needs and the one JPQL is said not
 * to be able to express.
 *
 * <p>It can. HQL 6 has window functions and derived tables in the from clause, which is
 * exactly what top N per group is made of, and the usual reason given for dropping to
 * native SQL here no longer applies.
 *
 * <p>The two single-statement variants both return {@code Tuple} rather than a DTO, on
 * purpose: a constructor expression exists in HQL and not in a native query, so mapping
 * one of them into a record and not the other would put a difference into the
 * comparison that has nothing to do with what is being compared.
 */
@Component
public class TopNPerGroupScenario extends AbstractScenario {

    private static final int TOP = 3;

    private final BookRepository books;

    public TopNPerGroupScenario(QueryProbe probe, BookRepository books) {
        super(probe);
        this.books = books;
    }

    @Override
    public String name() {
        return "top-n-per-group";
    }

    @Override
    public String question() {
        return "The " + TOP + " highest-rated reviews of every book.";
    }

    @Override
    protected String verdict() {
        return """
                Doing it in Java loses twice: it is the N+1 and it also materialises \
                every review in the catalogue to throw almost all of them away. Doing it \
                in JPQL one book at a time is the same N+1 with none of the entities, \
                which is better and still linear in the number of books. One statement \
                does the whole thing, and it does not have to be native SQL: HQL 6 \
                renders row_number() over (partition by ...) and accepts a derived table \
                in the from clause, so the HQL and the hand-written SQL render the same \
                window function and run in the same time. That is the finding - the folklore \
                that top N per group forces you out of JPQL is a Hibernate 5 memory.""";
    }

    @Override
    protected void variants(Runner runner, ProbeOptions options) {
        runner.problem("in-java",
                "Every book with every review loaded, then sorted and truncated in Java",
                options,
                session -> {
                    var all = books.findAllPlain();
                    List<Object> top = new ArrayList<>();
                    all.forEach(book -> book.getReviews().stream()
                            .sorted(Comparator.comparingInt(
                                    com.gkcontas.queryperf.catalog.Review::getRating).reversed())
                            .limit(TOP)
                            .forEach(top::add));
                    return top;
                });

        runner.problem("jpql-per-book",
                "One JPQL query per book with setMaxResults(" + TOP
                        + "): no wasted entities, same number of round trips",
                options,
                session -> {
                    List<Long> ids = session
                            .createQuery("select b.id from Book b order by b.id", Long.class)
                            .getResultList();
                    List<Object> top = new ArrayList<>();
                    for (Long id : ids) {
                        top.addAll(session.createQuery("""
                                        select r.id from Review r
                                        where r.book.id = :id
                                        order by r.rating desc, r.id
                                        """, Long.class)
                                .setParameter("id", id)
                                .setMaxResults(TOP)
                                .getResultList());
                    }
                    return top;
                });

        runner.fix("hql-window",
                "One HQL statement: row_number() over (partition by ...) inside a "
                        + "derived table in the from clause",
                options,
                session -> session.createQuery("""
                                select t.bookId, t.reviewId, t.rating, t.rn from (
                                    select r.book.id as bookId,
                                           r.id as reviewId,
                                           r.rating as rating,
                                           row_number() over (
                                               partition by r.book.id
                                               order by r.rating desc, r.id) as rn
                                    from Review r
                                ) t
                                where t.rn <= :top
                                order by t.bookId, t.rn
                                """, Tuple.class)
                        .setParameter("top", TOP)
                        .getResultList());

        runner.fix("native-window",
                "The same window function written by hand in SQL, for comparison",
                options,
                session -> session.createNativeQuery("""
                                SELECT book_id, id, rating, rn FROM (
                                    SELECT book_id, id, rating,
                                           row_number() OVER (
                                               PARTITION BY book_id
                                               ORDER BY rating DESC, id) AS rn
                                    FROM review
                                ) ranked
                                WHERE rn <= :top
                                ORDER BY book_id, rn
                                """, Tuple.class)
                        .setParameter("top", TOP)
                        .getResultList());
    }
}
