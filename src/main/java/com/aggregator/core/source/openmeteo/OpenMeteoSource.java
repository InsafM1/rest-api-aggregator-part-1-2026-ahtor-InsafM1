package com.aggregator.core.source.openmeteo;

import com.aggregator.core.http.HttpClientWrapper;
import com.aggregator.core.model.AggregatedItem;
import com.aggregator.core.source.ApiSource;
import com.aggregator.core.source.SourceException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OpenMeteoSource implements ApiSource {
    private static final String BASE_URL = "https://api.open-meteo.com/v1/forecast";

    private final HttpClientWrapper http;
    private final ObjectMapper mapper;

    public OpenMeteoSource(HttpClientWrapper http, ObjectMapper mapper) {
        this.http = http;
        this.mapper = mapper;
    }

    @Override
    public String getName() { return "open-meteo"; }

    @Override
    public List<AggregatedItem> fetch(Map<String, String> params) {
        String url = buildUrl(params);
        String json = http.get(url);
        try {
            OpenMeteoDto dto = mapper.readValue(json, OpenMeteoDto.class);
            return List.of(mapToItem(dto));
        } catch (Exception e) {
            throw new SourceException("Open-Meteo parse error", e);
        }
    }

    private String buildUrl(Map<String, String> params) {
        Map<String, String> finalParams = params.isEmpty()
                ? Map.of(
                "latitude", "52.52",
                "longitude", "13.41",
                "current", "temperature_2m,relative_humidity_2m,wind_speed_10m",
                "hourly", "temperature_2m")
                : params;

        String query = finalParams.entrySet().stream()
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));
        return BASE_URL + "?" + query;
    }

    private String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private AggregatedItem mapToItem(OpenMeteoDto dto) {
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("latitude", dto.latitude());
        attrs.put("longitude", dto.longitude());
        attrs.put("timezone", dto.timezone());
        if (dto.current() != null) {
            attrs.put("time", dto.current().time());
            attrs.put("temperature", dto.current().temperature_2m());
            attrs.put("humidity", dto.current().relative_humidity_2m());
            attrs.put("wind_speed", dto.current().wind_speed_10m());
        }
        if (dto.hourly() != null && dto.hourly().time() != null) {
            attrs.put("hourly_count", dto.hourly().time().size());
        }
        String title = "Weather at " + dto.latitude() + "," + dto.longitude();
        return new AggregatedItem(getName(), title, attrs, Instant.now());
    }
}
