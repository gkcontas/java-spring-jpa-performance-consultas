package com.gkcontas.queryperf.api;

import com.gkcontas.queryperf.measure.MeasurementStore;
import com.gkcontas.queryperf.measure.ProbeOptions;
import com.gkcontas.queryperf.scenario.Scenario;
import com.gkcontas.queryperf.scenario.ScenarioRegistry;
import com.gkcontas.queryperf.scenario.ScenarioReport;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/scenarios")
public class ScenarioController {

    private final ScenarioRegistry registry;
    private final MeasurementStore store;

    public ScenarioController(ScenarioRegistry registry, MeasurementStore store) {
        this.registry = registry;
        this.store = store;
    }

    @GetMapping
    public List<ScenarioView> list() {
        return registry.all().stream()
                .map(scenario -> new ScenarioView(scenario.name(), scenario.question()))
                .toList();
    }

    /**
     * Runs one scenario and stores the report.
     *
     * <p>{@code warmups} and {@code repeats} are on the route because a measurement
     * session wants more of both than a smoke test does, and because a reader who does
     * not trust the numbers should be able to raise them.
     */
    @PostMapping("/{name}")
    public ScenarioReport run(@PathVariable String name,
                              @RequestParam(defaultValue = "1") int warmups,
                              @RequestParam(defaultValue = "3") int repeats) {
        Scenario scenario = registry.find(name)
                .orElseThrow(() -> new ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "no scenario named '" + name + "'"));
        ScenarioReport report = scenario.run(new ProbeOptions(warmups, repeats, false));
        store.record(report);
        return report;
    }

    @PostMapping
    public List<ScenarioReport> runAll(@RequestParam(defaultValue = "1") int warmups,
                                       @RequestParam(defaultValue = "3") int repeats) {
        ProbeOptions options = new ProbeOptions(warmups, repeats, false);
        List<ScenarioReport> reports = registry.all().stream()
                .map(scenario -> scenario.run(options))
                .toList();
        reports.forEach(store::record);
        return reports;
    }

    @GetMapping("/{name}/report")
    public ResponseEntity<ScenarioReport> report(@PathVariable String name) {
        return store.find(name).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
