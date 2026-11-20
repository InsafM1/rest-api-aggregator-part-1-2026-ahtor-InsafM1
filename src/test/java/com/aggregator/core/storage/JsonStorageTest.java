package com.aggregator.core.storage;

import com.aggregator.core.model.AggregatedItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

public class JsonStorageTest {
    @TempDir
    Path tempDir;

    private JsonStorage storage;
    private AggregatedItem sampleItem;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        Path file = tempDir.resolve("test.json");
        storage = new JsonStorage(file, mapper);

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
    void shouldAppendWithoutLosingOldData() {
        storage.save(List.of(sampleItem), WriteMode.NEW);
        storage.save(List.of(sampleItem), WriteMode.APPEND);

        List<AggregatedItem> loaded = storage.load();
        assertThat(loaded).hasSize(2);
    }

    @Test
    void shouldReturnEmptyListWhenFileDoesNotExist() {
        List<AggregatedItem> loaded = storage.load();
        assertThat(loaded).isEmpty();
    }

    @Test
    void shouldFilterBySource() {
        AggregatedItem other = new AggregatedItem(
                "other-source", "Other", Map.of(), Instant.now());

        storage.save(List.of(sampleItem, other), WriteMode.NEW);
        List<AggregatedItem> filtered = storage.loadBySource("test-source");

        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).source()).isEqualTo("test-source");
    }

    @Test
    void shouldOverwriteWhenModeIsNew() {
        storage.save(List.of(sampleItem), WriteMode.NEW);
        storage.save(List.of(sampleItem), WriteMode.NEW);

        List<AggregatedItem> loaded = storage.load();
        assertThat(loaded).hasSize(1);
    }
}
