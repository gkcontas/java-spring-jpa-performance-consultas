package com.gkcontas.queryperf.api;

import com.gkcontas.queryperf.dataset.DatasetSeeder;
import com.gkcontas.queryperf.dataset.SeedProperties;
import com.gkcontas.queryperf.dataset.SeedReport;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dataset")
public class DatasetController {

    private final DatasetSeeder seeder;
    private final SeedProperties configured;
    private final JdbcTemplate jdbc;

    public DatasetController(DatasetSeeder seeder, SeedProperties configured, JdbcTemplate jdbc) {
        this.seeder = seeder;
        this.configured = configured;
        this.jdbc = jdbc;
    }

    /** Seeds the configured shape. */
    @PostMapping
    public SeedReport seed() {
        return seeder.seed();
    }

    /** Seeds a shape given in the request, for measuring the same scenarios at another size. */
    @PostMapping("/custom")
    public SeedReport seed(@Valid @RequestBody SeedRequest request) {
        return seeder.seed(new SeedProperties(
                request.publishers(), request.authors(), request.books(),
                request.reviewsPerBook(), request.authorsPerBook(), request.coverRatio(),
                configured.batchSize()));
    }

    @GetMapping
    public DatasetView counts() {
        return new DatasetView(
                count("publisher"), count("author"), count("book"), count("review"),
                count("book_author"), count("book_cover"));
    }

    @DeleteMapping
    public ResponseEntity<Void> truncate() {
        seeder.truncate();
        return ResponseEntity.noContent().build();
    }

    private long count(String table) {
        Long value = jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
        return value == null ? 0 : value;
    }
}
