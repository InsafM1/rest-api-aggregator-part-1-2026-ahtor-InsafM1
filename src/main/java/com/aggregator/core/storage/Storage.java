package com.aggregator.core.storage;

import com.aggregator.core.model.AggregatedItem;
import com.aggregator.core.source.WriteMode;

import java.util.List;

public interface Storage {
    void save(List<AggregatedItem> items, WriteMode mode);
    List<AggregatedItem> load();
    List<AggregatedItem> loadBySource(String source);
}