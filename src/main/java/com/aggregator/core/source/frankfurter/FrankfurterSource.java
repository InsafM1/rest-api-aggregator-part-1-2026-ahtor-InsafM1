package com.aggregator.core.source.frankfurter;

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

public class FrankfurterSource implements ApiSource {

    private static final String BASE_URL = "https://api.frankfurter.app/latest";

    private final HttpClientWrapper http;
    private final ObjectMapper mapper;

    public FrankfurterSource(HttpClientWrapper http, ObjectMapper mapper) {
        this.http = http;
        this.mapper = mapper;
    }

    @Override
    public String getName() { return "frankfurter"; }

    @Override
    public List<AggregatedItem> fetch(Map<String, String> params) {
        String url = buildUrl(params);
        String json = http.get(url);
        try {
            FrankfurterDto dto = mapper.readValue(json, FrankfurterDto.class);
            return List.of(mapToItem(dto));
        } catch (Exception e) {
            throw new SourceException("Frankfurter parse error", e);
        }
    }

    private String buildUrl(Map<String, String> params) {
        Map<String, String> finalParams = params.isEmpty()
                ? Map.of("base", "USD", "symbols", "EUR,GBP")
                : params;

        String query = finalParams.entrySet().stream()
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));
        return BASE_URL + "?" + query;
    }

    private String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private AggregatedItem mapToItem(FrankfurterDto dto) {
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("base", dto.base());
        attrs.put("amount", dto.amount());
        attrs.put("date", dto.date());
        if (dto.rates() != null) {
            attrs.putAll(dto.rates());
        }
        String title = "Rates for " + dto.base() + " on " + dto.date();
        return new AggregatedItem(getName(), title, attrs, Instant.now());
    }
}
