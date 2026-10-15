package com.gkcontas.queryperf.measure;

/**
 * How many times to run a variant before believing the clock.
 *
 * @param warmups runs whose duration is thrown away. The first execution of a query
 *     pays for the JIT, the Hibernate query plan and the PostgreSQL plan cache, none
 *     of which is what the scenario is about
 * @param repeats timed runs; the reported duration is their median
 * @param keepCaches when false, both levels of cache are emptied before every single
 *     run, so a variant never inherits a warm cache from the variant before it. Only
 *     the cache scenario sets it to true, because for that one the warm cache is the
 *     measurement
 */
public record ProbeOptions(int warmups, int repeats, boolean keepCaches) {

    public static ProbeOptions defaults() {
        return new ProbeOptions(1, 3, false);
    }

    /**
     * Keeps both levels of cache between runs, and guarantees at least one warm-up.
     *
     * <p>The warm-up is not optional here: a cache measured on its first read is not
     * being measured, and a scenario about caching must not depend on the caller having
     * asked for warm-ups.
     */
    public ProbeOptions keepingCaches() {
        return new ProbeOptions(Math.max(1, warmups), repeats, true);
    }

    /** For variants whose cost is dominated by round trips and is not worth timing thrice. */
    public static ProbeOptions single() {
        return new ProbeOptions(0, 1, false);
    }
}
