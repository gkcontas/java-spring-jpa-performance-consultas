package com.gkcontas.queryperf.measure;

import java.util.Collection;
import org.hibernate.Session;

/**
 * A unit of work to be measured.
 *
 * <p>It receives the {@link Session} of the transaction the probe opened, because some
 * of the repairs are session-side rather than query-side: {@code setFetchBatchSize} and
 * {@code setSubselectFetchingEnabled} change how a lazy collection is fetched without
 * touching the entity or the query. Variants that go through Spring Data repositories
 * ignore the argument.
 *
 * <p>The return value is a collection so the probe can record how many objects the
 * variant produced. A variant that answers a smaller question is not a cheaper variant.
 */
@FunctionalInterface
public interface ProbedWork {

    Collection<?> execute(Session session);
}
