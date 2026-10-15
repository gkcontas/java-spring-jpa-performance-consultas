package com.gkcontas.queryperf.scenario;

import com.gkcontas.queryperf.measure.Measurement;
import java.time.Instant;
import java.util.List;

/**
 * One scenario, run end to end.
 *
 * @param scenario the identifier used on the route
 * @param question what is being asked of the database, in words, so the variants can be
 *     compared without having to assume they do the same work
 * @param verdict the conclusion the numbers support
 * @param variants the problem first, then the repairs, in the order they were run
 */
public record ScenarioReport(
        String scenario,
        String question,
        String verdict,
        List<Measurement> variants,
        Instant ranAt) {}
