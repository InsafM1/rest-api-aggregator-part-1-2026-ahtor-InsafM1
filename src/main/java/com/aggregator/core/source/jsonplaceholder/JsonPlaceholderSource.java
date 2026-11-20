package com.aggregator.core.source.jsonplaceholder;

import com.aggregator.core.http.HttpClientWrapper;
import com.aggregator.core.model.AggregatedItem;
import com.aggregator.core.source.ApiSource;
import com.aggregator.core.source.SourceException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JsonPlaceholderSource implements ApiSource {

    private static final String BASE_URL = "https://jsonplaceholder.typicode.com/posts";

    private final HttpClientWrapper http;
    private final ObjectMapper mapper;

    public JsonPlaceholderSource(HttpClientWrapper http, ObjectMapper mapper) {
        this.http = http;
        this.mapper = mapper;
    }

    @Override
    public String getName() { return "jsonplaceholder"; }

    @Override
    public List<AggregatedItem> fetch(Map<String, String> params) {
        String url = buildUrl(params);
        String json = http.get(url);
        try {
            List<JsonPlaceholderDto> dtos = mapper.readValue(json,
                    new TypeReference<List<JsonPlaceholderDto>>() {});

            List<AggregatedItem> items = new ArrayList<>();
            for (JsonPlaceholderDto dto : dtos) {
                items.add(mapToItem(dto));
            }
            return items;
        } catch (Exception e) {
            throw new SourceException("JSONPlaceholder parse error", e);
        }
    }

    private String buildUrl(Map<String, String> params) {
        Map<String, String> finalParams = params.isEmpty()
                ? Map.of("_limit", "5")
                : params;

        String query = finalParams.entrySet().stream()
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));
        return BASE_URL + "?" + query;
    }

    private String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private AggregatedItem mapToItem(JsonPlaceholderDto dto) {
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("userId", dto.userId());
        attrs.put("id", dto.id());
        attrs.put("body", dto.body());
        return new AggregatedItem(getName(), dto.title(), attrs, Instant.now());
    }
}
