package com.aggregator.core.source;

import com.aggregator.core.model.AggregatedItem;
import java.util.List;
import java.util.Map;

public interface ApiSource {
    String getName();
    List<AggregatedItem> fetch(Map<String, String> params);
}