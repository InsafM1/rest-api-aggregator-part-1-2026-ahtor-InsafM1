package com.aggregator.core.model;

import java.time.Instant;
import java.util.Map;

public record AggregatedItem(
        String source,
        String title,
        Map<String, Object> attributes,
        Instant fetchedAt
) {}