package com.aggregator.core.storage;

import com.aggregator.core.model.AggregatedItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

public class CsvStorageTest {
    @TempDir
    Path tempDir;

    private CsvStorage storage;
    private Path file;
    private AggregatedItem sampleItem;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        file = tempDir.resolve("test.csv");
        storage = new CsvStorage(file, mapper);

        sampleItem = new AggregatedItem(
                "test-source",
                "Test Title",
                Map.of("key", "value", "number", 42),
                Instant.parse("2026-11-20T12:00:00Z")
        );
    }

    @Test
    void shouldSaveAndLoadItems() {
        storage.save(List.of(sampleItem), WriteMode.NEW);
        List<AggregatedItem> loaded = storage.load();

        assertThat(loaded).hasSize(1);
        AggregatedItem item = loaded.get(0);
        assertThat(item.source()).isEqualTo("test-source");
        assertThat(item.title()).isEqualTo("Test Title");
        assertThat(item.attributes()).containsEntry("key", "value");
        assertThat(item.fetchedAt()).isEqualTo(sampleItem.fetchedAt());
    }

    @Test
    void shouldWriteHeaderOnlyOnce() throws Exception {
        storage.save(List.of(sampleItem), WriteMode.NEW);
        storage.save(List.of(sampleItem), WriteMode.APPEND);

        List<String> lines = Files.readAllLines(file);
        assertThat(lines).hasSize(3);
        assertThat(lines.get(0)).startsWith("source,title,fetchedAt,attributes");
        assertThat(lines.get(1)).doesNotContain("source,title");
        assertThat(lines.get(2)).doesNotContain("source,title");
    }

    @Test
    void shouldNotLoseDataOnAppend() {
        storage.save(List.of(sampleItem), WriteMode.NEW);
        storage.save(List.of(sampleItem), WriteMode.APPEND);

        List<AggregatedItem> loaded = storage.load();
        assertThat(loaded).hasSize(2);
    }

    @Test
    void shouldFilterBySource() {
        AggregatedItem other = new AggregatedItem(
                "other-source", "Other", Map.of(), Instant.now());

        storage.save(List.of(sampleItem, other), WriteMode.NEW);
        List<AggregatedItem> filtered = storage.loadBySource("test-source");

        assertThat(filtered).hasSize(1);
    }

    @Test
    void shouldOverwriteWhenModeIsNew() {
        storage.save(List.of(sampleItem), WriteMode.NEW);
        storage.save(List.of(sampleItem), WriteMode.NEW);

        List<AggregatedItem> loaded = storage.load();
        assertThat(loaded).hasSize(1);
    }
}
