package com.gkcontas.queryperf.api;

import com.gkcontas.queryperf.measure.MeasurementStore;
import com.gkcontas.queryperf.scenario.ScenarioReport;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/measurements")
public class MeasurementController {

    private final MeasurementStore store;

    public MeasurementController(MeasurementStore store) {
        this.store = store;
    }

    @GetMapping
    public List<ScenarioReport> all() {
        return store.all();
    }

    @DeleteMapping
    public ResponseEntity<Void> clear() {
        store.clear();
        return ResponseEntity.noContent().build();
    }
}
