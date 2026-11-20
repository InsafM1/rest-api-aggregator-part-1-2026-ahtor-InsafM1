package com.aggregator.core.source.jsonplaceholder;

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

public class JsonPlaceholderSourceTest {
    private HttpClientWrapper httpMock;
    private JsonPlaceholderSource source;

    @BeforeEach
    void setUp() {
        httpMock = mock(HttpClientWrapper.class);
        source = new JsonPlaceholderSource(httpMock, new ObjectMapper());
    }

    @Test
    void shouldParseArrayAndReturnMultipleItems() {
        String fakeJson = """
                [
                  {"userId": 1, "id": 1, "title": "first post", "body": "body one"},
                  {"userId": 2, "id": 2, "title": "second post", "body": "body two"}
                ]
                """;
        when(httpMock.get(anyString())).thenReturn(fakeJson);
        List<AggregatedItem> items = source.fetch(Map.of());
        assertThat(items).hasSize(2);

        AggregatedItem first = items.get(0);
        assertThat(first.source()).isEqualTo("jsonplaceholder");
        assertThat(first.title()).isEqualTo("first post");
        assertThat(first.attributes())
                .containsEntry("userId", 1)
                .containsEntry("id", 1)
                .containsEntry("body", "body one");

        AggregatedItem second = items.get(1);
        assertThat(second.title()).isEqualTo("second post");
        assertThat(second.attributes()).containsEntry("userId", 2);
    }

    @Test
    void shouldReturnEmptyListOnEmptyArray() {
        when(httpMock.get(anyString())).thenReturn("[]");
        List<AggregatedItem> items = source.fetch(Map.of());
        assertThat(items).isEmpty();
    }

    @Test
    void shouldUseDefaultLimitParam() {
        when(httpMock.get(anyString())).thenReturn("[]");
        source.fetch(Map.of());
        verify(httpMock).get(contains("_limit=5"));
    }

    @Test
    void shouldUseCustomParams() {
        when(httpMock.get(anyString())).thenReturn("[]");
        source.fetch(Map.of("_limit", "10", "userId", "3"));
        verify(httpMock).get(contains("_limit=10"));
        verify(httpMock).get(contains("userId=3"));
    }

    @Test
    void shouldThrowSourceExceptionOnInvalidJson() {
        when(httpMock.get(anyString())).thenReturn("{ not an array }");
        assertThatThrownBy(() -> source.fetch(Map.of()))
                .isInstanceOf(SourceException.class)
                .hasMessageContaining("JSONPlaceholder parse error");
    }
}
