package com.gkcontas.queryperf.measure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs a unit of work and reports what it cost the database.
 *
 * <p>Three things have to be true for a number here to mean anything, and all three are
 * the probe's job rather than the scenario's.
 *
 * <p><b>A fresh persistence context.</b> Each run gets its own transaction, so each gets
 * its own first-level cache. Run two variants in one transaction and the second one is
 * free: the entities are already managed and Hibernate answers from the session without
 * going anywhere.
 *
 * <p><b>A cold second-level cache,</b> unless the scenario is about the cache. Leaving it
 * warm would make whichever variant happened to run second look like the fix.
 *
 * <p><b>Counters read after the transaction commits,</b> not inside it. A flush at commit
 * can send statements of its own, and a unit of work that leaves work for the commit is
 * not cheaper than one that does it up front.
 */
@Component
public class QueryProbe {

    private final EntityManagerFactory entityManagerFactory;
    private final TransactionTemplate transactionTemplate;

    public QueryProbe(EntityManagerFactory entityManagerFactory,
                      PlatformTransactionManager transactionManager) {
        this.entityManagerFactory = entityManagerFactory;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public Measurement measure(String variant,
                               VariantKind kind,
                               String explanation,
                               ProbeOptions options,
                               ProbedWork work) {
        for (int i = 0; i < options.warmups(); i++) {
            runOnce(options, work);
        }

        List<Long> durations = new ArrayList<>();
        Run last = null;
        for (int i = 0; i < Math.max(1, options.repeats()); i++) {
            last = runOnce(options, work);
            durations.add(last.millis());
        }

        return new Measurement(
                variant,
                kind,
                explanation,
                last.statistics().statements(),
                last.statistics().entitiesLoaded(),
                last.statistics().collectionsLoaded(),
                last.statistics().secondLevelHits(),
                last.statistics().secondLevelMisses(),
                last.statistics().queryCacheHits(),
                last.statistics().queryCacheMisses(),
                last.resultSize(),
                median(durations),
                last.failure(),
                collapse(last.sql()));
    }

    private Run runOnce(ProbeOptions options, ProbedWork work) {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        if (!options.keepCaches()) {
            evictEverything(sessionFactory);
        }
        Statistics statistics = sessionFactory.getStatistics();
        statistics.clear();
        StatementRecorder.start();

        long startedAt = System.nanoTime();
        Integer resultSize = 0;
        String failure = null;
        try {
            resultSize = transactionTemplate.execute(status -> {
                EntityManager entityManager = EntityManagerFactoryUtils
                        .getTransactionalEntityManager(entityManagerFactory);
                Session session = entityManager.unwrap(Session.class);
                Collection<?> produced = work.execute(session);
                return produced == null ? 0 : produced.size();
            });
        } catch (RuntimeException thrown) {
            // A variant that cannot run is a result, not a broken measurement: two of
            // the ones compared here fail by design, and the exception they fail with
            // is the thing worth reporting.
            failure = describe(thrown);
        }
        long millis = (System.nanoTime() - startedAt) / 1_000_000;

        List<String> sql = StatementRecorder.stop();
        return new Run(snapshot(statistics), resultSize == null ? 0 : resultSize, millis,
                failure, sql);
    }

    private void evictEverything(SessionFactory sessionFactory) {
        sessionFactory.getCache().evictAllRegions();
        sessionFactory.getCache().evictQueryRegions();
    }

    private static Counters snapshot(Statistics statistics) {
        return new Counters(
                statistics.getPrepareStatementCount(),
                statistics.getEntityLoadCount(),
                statistics.getCollectionLoadCount(),
                statistics.getSecondLevelCacheHitCount(),
                statistics.getSecondLevelCacheMissCount(),
                statistics.getQueryCacheHitCount(),
                statistics.getQueryCacheMissCount());
    }

    private static long median(List<Long> values) {
        List<Long> sorted = new ArrayList<>(values);
        sorted.sort(Long::compareTo);
        return sorted.get(sorted.size() / 2);
    }

    /**
     * Collapses repetitions of the same statement, keeping the order of first
     * appearance. An N+1 of two thousand round trips is two lines here, which is the
     * only form in which it is readable.
     */
    private static List<SqlStatement> collapse(List<String> statements) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String sql : statements) {
            counts.merge(sql, 1, Integer::sum);
        }
        List<SqlStatement> collapsed = new ArrayList<>();
        counts.forEach((sql, times) ->
                collapsed.add(new SqlStatement(sql, times, SelectWidth.of(sql))));
        return List.copyOf(collapsed);
    }

    private record Counters(long statements,
                            long entitiesLoaded,
                            long collectionsLoaded,
                            long secondLevelHits,
                            long secondLevelMisses,
                            long queryCacheHits,
                            long queryCacheMisses) {}

    private record Run(Counters statistics,
                       int resultSize,
                       long millis,
                       String failure,
                       List<String> sql) {}

    /** Root cause and message, which is where the useful half of a JPA stack trace is. */
    private static String describe(Throwable thrown) {
        Throwable root = thrown;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getClass().getSimpleName() + ": " + root.getMessage();
    }
}
