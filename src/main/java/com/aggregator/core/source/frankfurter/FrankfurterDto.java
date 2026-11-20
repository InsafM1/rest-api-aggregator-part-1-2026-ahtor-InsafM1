package com.aggregator.core.source.frankfurter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FrankfurterDto(
        double amount,
        String base,
        String date,
        Map<String, Double> rates
) {
}
