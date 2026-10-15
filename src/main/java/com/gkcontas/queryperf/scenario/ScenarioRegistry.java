package com.gkcontas.queryperf.scenario;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ScenarioRegistry {

    private final Map<String, Scenario> byName = new LinkedHashMap<>();

    public ScenarioRegistry(List<Scenario> scenarios) {
        scenarios.stream()
                .sorted(java.util.Comparator.comparing(Scenario::name))
                .forEach(scenario -> byName.put(scenario.name(), scenario));
    }

    public List<Scenario> all() {
        return List.copyOf(byName.values());
    }

    public Optional<Scenario> find(String name) {
        return Optional.ofNullable(byName.get(name));
    }
}
