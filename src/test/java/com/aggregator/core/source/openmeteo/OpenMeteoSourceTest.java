package com.aggregator.core.source.openmeteo;

import com.aggregator.core.http.HttpClientWrapper;
import com.aggregator.core.model.AggregatedItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class OpenMeteoSourceTest {
    private HttpClientWrapper httpMock;
    private ObjectMapper mapper;
    private OpenMeteoSource source;

    @BeforeEach
    void setUp() {
        httpMock = mock(HttpClientWrapper.class);
        mapper = new ObjectMapper();
        source = new OpenMeteoSource(httpMock, mapper);
    }

    @Test
    void shouldParseValidJsonAndReturnAggregatedItem() {
        String fakeJson = """
                {
                  "latitude": 52.52,
                  "longitude": 13.41,
                  "timezone": "GMT",
                  "current": {
                    "time": "2026-11-20T12:00",
                    "temperature_2m": 7.3,
                    "relative_humidity_2m": 85,
                    "wind_speed_10m": 12.5
                  }
                }
                """;
        when(httpMock.get(anyString())).thenReturn(fakeJson);
        List<AggregatedItem> items = source.fetch(Map.of());
        assertThat(items).hasSize(1);
        AggregatedItem item = items.get(0);
        assertThat(item.source()).isEqualTo("open-meteo");
        assertThat(item.title()).isEqualTo("Weather at 52.52,13.41");
        assertThat(item.attributes()).containsEntry("temperature", 7.3);
        assertThat(item.attributes()).containsEntry("humidity", 85);
        verify(httpMock, times(1)).get(contains("latitude=52.52"));
    }

    @Test
    void shouldThrowSourceExceptionWhenJsonIsInvalid() {
        when(httpMock.get(anyString())).thenReturn("{ invalid json ");
        org.junit.jupiter.api.Assertions.assertThrows(
                com.aggregator.core.source.SourceException.class,
                () -> source.fetch(Map.of())
        );
    }
}
