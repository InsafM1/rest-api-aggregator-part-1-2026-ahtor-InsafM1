package com.aggregator.core.storage;

import com.aggregator.core.model.AggregatedItem;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class JsonStorage implements Storage {

    private static final Logger log = LoggerFactory.getLogger(JsonStorage.class);

    private final Path file;
    private final ObjectMapper mapper;

    public JsonStorage(Path file, ObjectMapper mapper) {
        this.file = file;
        this.mapper = mapper;
    }

    @Override
    public void save(List<AggregatedItem> items, WriteMode mode) {
        List<AggregatedItem> allItems = new ArrayList<>();
        if (mode == WriteMode.APPEND && Files.exists(file)) {
            allItems.addAll(load());
        }

        allItems.addAll(items);

        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }

            mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), allItems);
            log.info("Saved {} items to {} (mode={})", items.size(), file, mode);
        } catch (IOException e) {
            throw new StorageException("JSON save failed: " + file, e);
        }
    }

    @Override
    public List<AggregatedItem> load() {
        if (!Files.exists(file)) {
            return List.of();
        }

        try {
            return mapper.readValue(file.toFile(), new TypeReference<List<AggregatedItem>>() {});
        } catch (IOException e) {
            throw new StorageException("JSON load failed: " + file, e);
        }
    }

    @Override
    public List<AggregatedItem> loadBySource(String source) {
        return load().stream()
                .filter(item -> item.source().equals(source))
                .toList();
    }
}
