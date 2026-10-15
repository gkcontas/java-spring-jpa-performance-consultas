package com.gkcontas.queryperf;

import com.gkcontas.queryperf.measure.Measurement;
import com.gkcontas.queryperf.measure.ProbeOptions;
import com.gkcontas.queryperf.scenario.Scenario;
import com.gkcontas.queryperf.scenario.ScenarioRegistry;
import com.gkcontas.queryperf.scenario.ScenarioReport;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Runs one scenario against the fixed test catalogue and hands its variants to the
 * assertions.
 *
 * <p>The counts asserted downstream are exact, not bounds. That is the point of having
 * them: the README quotes statement counts, and an exact assertion is what stops a
 * mapping change from quietly turning one of its claims into a lie.
 */
public abstract class ScenarioTestBase extends IntegrationTestBase {

    @Autowired
    private ScenarioRegistry registry;

    protected ScenarioReport report;

    protected abstract String scenario();

    @BeforeEach
    void runScenario() {
        seedOnce();
        Scenario subject = registry.find(scenario())
                .orElseThrow(() -> new AssertionError("no scenario " + scenario()));
        report = subject.run(new ProbeOptions(1, 1, false));
    }

    protected Measurement of(String variant) {
        return variant(report, variant);
    }

    /** Books in the test catalogue, which is also the N of every N+1 here. */
    protected static int books() {
        return TEST_SHAPE.books();
    }

    protected static int reviews() {
        return TEST_SHAPE.books() * TEST_SHAPE.reviewsPerBook();
    }
}
