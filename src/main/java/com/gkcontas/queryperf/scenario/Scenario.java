package com.gkcontas.queryperf.scenario;

import com.gkcontas.queryperf.measure.ProbeOptions;

/**
 * A question put to the database more than one way, with the cost of each way measured.
 *
 * <p>Every scenario has at least one {@code PROBLEM} variant and at least one
 * {@code FIX}, and all of its variants have to produce the same answer - otherwise the
 * comparison is between a query and a different, smaller query.
 */
public interface Scenario {

    String name();

    String question();

    ScenarioReport run(ProbeOptions options);
}
