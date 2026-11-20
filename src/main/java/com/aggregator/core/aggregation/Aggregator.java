package com.aggregator.core.aggregation;

import com.aggregator.core.model.AggregatedItem;
import com.aggregator.core.source.ApiSource;
import com.aggregator.core.source.SourceException;
import com.aggregator.core.storage.Storage;
import com.aggregator.core.storage.WriteMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Aggregator {
    private static final Logger log = LoggerFactory.getLogger(Aggregator.class);
    private final List<ApiSource> sources;

    public Aggregator(List<ApiSource> sources) {
        this.sources = sources;
    }

    public AggregationResult poll(List<String> sourceNames,
                                  Map<String, Map<String, String>> paramsBySource,
                                  Storage storage,
                                  WriteMode mode) {
        List<AggregatedItem> collected = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (ApiSource source : sources) {
            if (!sourceNames.isEmpty() && !sourceNames.contains(source.getName())) {
                continue;
            }
            Map<String, String> params = paramsBySource.getOrDefault(source.getName(), Map.of());
            log.info("Polling source={} params={}", source.getName(), params);
            try {
                List<AggregatedItem> items = source.fetch(params);
                log.info("Source {} returned {} items", source.getName(), items.size());
                collected.addAll(items);
            } catch (SourceException e) {
                log.warn("Source {} failed: {}", source.getName(), e.getMessage());
                errors.add(source.getName() + ": " + e.getMessage());
            }
        }

        storage.save(collected, mode);
        log.info("Saved {} items, {} errors", collected.size(), errors.size());
        return new AggregationResult(collected.size(), errors);
    }
}
