package com.aggregator.core.source.openmeteo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenMeteoDto(
        double latitude,
        double longitude,
        String timezone,
        Current current,
        Hourly hourly
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Current(
            String time,
            double temperature_2m,
            int relative_humidity_2m,
            double wind_speed_10m
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Hourly(
            List<String> time,
            List<Double> temperature_2m
    ) {}
}