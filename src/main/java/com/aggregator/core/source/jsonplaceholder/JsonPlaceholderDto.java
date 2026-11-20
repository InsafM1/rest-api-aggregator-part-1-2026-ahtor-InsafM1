package com.aggregator.core.source.jsonplaceholder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JsonPlaceholderDto(
        int userId,
        int id,
        String title,
        String body
) {}
