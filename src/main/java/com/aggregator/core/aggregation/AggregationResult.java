package com.aggregator.core.aggregation;

import java.util.List;

public record AggregationResult(int saved, List<String> errors) {
}
