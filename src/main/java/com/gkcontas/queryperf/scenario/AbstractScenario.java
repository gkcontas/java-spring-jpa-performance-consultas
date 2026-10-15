package com.gkcontas.queryperf.scenario;

import com.gkcontas.queryperf.measure.Measurement;
import com.gkcontas.queryperf.measure.ProbeOptions;
import com.gkcontas.queryperf.measure.ProbedWork;
import com.gkcontas.queryperf.measure.QueryProbe;
import com.gkcontas.queryperf.measure.VariantKind;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Boilerplate shared by every scenario: run variants in order, collect, report. */
public abstract class AbstractScenario implements Scenario {

    protected final QueryProbe probe;

    protected AbstractScenario(QueryProbe probe) {
        this.probe = probe;
    }

    protected abstract String verdict();

    protected abstract void variants(Runner runner, ProbeOptions options);

    @Override
    public ScenarioReport run(ProbeOptions options) {
        Runner runner = new Runner();
        variants(runner, options);
        return new ScenarioReport(name(), question(), verdict(),
                List.copyOf(runner.collected), Instant.now());
    }

    /** Collects the variants of one run, keeping the order they were declared in. */
    protected final class Runner {

        private final List<Measurement> collected = new ArrayList<>();

        public void problem(String variant, String explanation, ProbeOptions options,
                            ProbedWork work) {
            add(variant, VariantKind.PROBLEM, explanation, options, work);
        }

        public void fix(String variant, String explanation, ProbeOptions options,
                        ProbedWork work) {
            add(variant, VariantKind.FIX, explanation, options, work);
        }

        public void trap(String variant, String explanation, ProbeOptions options,
                         ProbedWork work) {
            add(variant, VariantKind.TRAP, explanation, options, work);
        }

        private void add(String variant, VariantKind kind, String explanation,
                         ProbeOptions options, ProbedWork work) {
            collected.add(probe.measure(variant, kind, explanation, options, work));
        }
    }
}
