package com.gkcontas.queryperf.measure;

import com.gkcontas.queryperf.scenario.ScenarioReport;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** The last report of each scenario, so a run can be read back without running again. */
@Component
public class MeasurementStore {

    private final Map<String, ScenarioReport> reports = new ConcurrentHashMap<>();

    public void record(ScenarioReport report) {
        reports.put(report.scenario(), report);
    }

    public Optional<ScenarioReport> find(String scenario) {
        return Optional.ofNullable(reports.get(scenario));
    }

    public List<ScenarioReport> all() {
        return reports.values().stream()
                .sorted(java.util.Comparator.comparing(ScenarioReport::scenario))
                .toList();
    }

    public void clear() {
        reports.clear();
    }
}
