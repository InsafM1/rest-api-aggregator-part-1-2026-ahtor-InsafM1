package com.aggregator.core.source.frankfurter;

import com.aggregator.core.http.HttpClientWrapper;
import com.aggregator.core.model.AggregatedItem;
import com.aggregator.core.source.SourceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

public class FrankfurterSourceTest {
    private HttpClientWrapper httpMock;
    private FrankfurterSource source;

    @BeforeEach
    void setUp() {
        httpMock = mock(HttpClientWrapper.class);
        source = new FrankfurterSource(httpMock, new ObjectMapper());
    }

    @Test
    void shouldParseValidJsonAndReturnAggregatedItem() {
        String fakeJson = """
                {
                  "amount": 1.0,
                  "base": "USD",
                  "date": "2026-11-20",
                  "rates": {
                    "EUR": 0.92,
                    "GBP": 0.79
                  }
                }
                """;
        when(httpMock.get(anyString())).thenReturn(fakeJson);
        List<AggregatedItem> items = source.fetch(Map.of());
        assertThat(items).hasSize(1);
        AggregatedItem item = items.get(0);

        assertThat(item.source()).isEqualTo("frankfurter");
        assertThat(item.title()).isEqualTo("Rates for USD on 2026-11-20");
        assertThat(item.attributes())
                .containsEntry("base", "USD")
                .containsEntry("amount", 1.0)
                .containsEntry("date", "2026-11-20")
                .containsEntry("EUR", 0.92)
                .containsEntry("GBP", 0.79);
    }

    @Test
    void shouldUseDefaultParamsWhenNoParamsProvided() {
        when(httpMock.get(anyString())).thenReturn("{}");
        source.fetch(Map.of());
        verify(httpMock).get(contains("base=USD"));
        verify(httpMock).get(contains("symbols=EUR%2CGBP"));
    }

    @Test
    void shouldUseCustomParams() {
        when(httpMock.get(anyString())).thenReturn("{}");
        source.fetch(Map.of("base", "EUR", "symbols", "USD,JPY"));
        verify(httpMock).get(contains("base=EUR"));
        verify(httpMock).get(contains("symbols=USD%2CJPY"));
    }

    @Test
    void shouldThrowSourceExceptionOnInvalidJson() {
        when(httpMock.get(anyString())).thenReturn("not a json");
        assertThatThrownBy(() -> source.fetch(Map.of()))
                .isInstanceOf(SourceException.class)
                .hasMessageContaining("Frankfurter parse error");
    }
}
