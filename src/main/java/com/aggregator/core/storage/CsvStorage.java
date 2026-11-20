package com.aggregator.core.storage;

import com.aggregator.core.model.AggregatedItem;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CsvStorage implements Storage {
    private static final Logger log = LoggerFactory.getLogger(CsvStorage.class);
    private static final String[] HEADER = {"source", "title", "fetchedAt", "attributes"};
    private final Path file;
    private final ObjectMapper mapper;

    public CsvStorage(Path file, ObjectMapper mapper) {
        this.file = file;
        this.mapper = mapper;
    }

    @Override
    public void save(List<AggregatedItem> items, WriteMode mode) {
        boolean exists = Files.exists(file);
        boolean writeHeader = (mode == WriteMode.NEW) || !exists;

        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }

            StandardOpenOption[] options = writeHeader
                    ? new StandardOpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE}
                    : new StandardOpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE};

            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8, options);
                 CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                         .setHeader(HEADER)
                         .setSkipHeaderRecord(!writeHeader)
                         .build())) {

                for (AggregatedItem item : items) {
                    printer.printRecord(
                            item.source(),
                            item.title(),
                            item.fetchedAt().toString(),
                            mapper.writeValueAsString(item.attributes())
                    );
                }
            }
            log.info("Saved {} items to {} (mode={})", items.size(), file, mode);
        } catch (IOException e) {
            throw new StorageException("CSV save failed: " + file, e);
        }
    }

    @Override
    public List<AggregatedItem> load() {
        if (!Files.exists(file)) {
            return List.of();
        }

        List<AggregatedItem> result = new ArrayList<>();

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader(HEADER)
                     .setSkipHeaderRecord(true)
                     .build()
                     .parse(reader)) {

            for (CSVRecord record : parser) {
                result.add(new AggregatedItem(
                        record.get("source"),
                        record.get("title"),
                        mapper.readValue(record.get("attributes"), new TypeReference<Map<String, Object>>() {}),
                        Instant.parse(record.get("fetchedAt"))
                ));
            }
            return result;
        } catch (IOException e) {
            throw new StorageException("CSV load failed: " + file, e);
        }
    }

    @Override
    public List<AggregatedItem> loadBySource(String source) {
        return load().stream()
                .filter(item -> item.source().equals(source))
                .toList();
    }
}
