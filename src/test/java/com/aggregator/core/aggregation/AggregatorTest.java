package com.aggregator.core.aggregation;

import com.aggregator.core.model.AggregatedItem;
import com.aggregator.core.source.ApiSource;
import com.aggregator.core.source.SourceException;
import com.aggregator.core.storage.Storage;
import com.aggregator.core.storage.WriteMode;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class AggregatorTest {
    @Test
    void shouldCollectDataFromAllSourcesAndSave() {
        ApiSource source1 = mock(ApiSource.class);
        ApiSource source2 = mock(ApiSource.class);
        when(source1.getName()).thenReturn("source1");
        when(source2.getName()).thenReturn("source2");
        when(source1.fetch(any())).thenReturn(List.of(createItem("source1")));
        when(source2.fetch(any())).thenReturn(List.of(createItem("source2")));

        Storage storage = mock(Storage.class);

        Aggregator aggregator = new Aggregator(List.of(source1, source2));

        AggregationResult result = aggregator.poll(
                List.of(),
                Map.of(),
                storage,
                WriteMode.NEW
        );

        assertThat(result.saved()).isEqualTo(2);
        assertThat(result.errors()).isEmpty();

        verify(storage).save(argThat(list -> list.size() == 2), eq(WriteMode.NEW));
    }

    @Test
    void shouldContinueWhenOneSourceFails() {
        ApiSource goodSource = mock(ApiSource.class);
        ApiSource badSource = mock(ApiSource.class);

        when(goodSource.getName()).thenReturn("good");
        when(goodSource.fetch(any())).thenReturn(List.of(createItem("good")));

        when(badSource.getName()).thenReturn("bad");
        when(badSource.fetch(any())).thenThrow(new SourceException("Network error"));

        Storage storage = mock(Storage.class);
        Aggregator aggregator = new Aggregator(List.of(goodSource, badSource));

        AggregationResult result = aggregator.poll(List.of(), Map.of(), storage, WriteMode.NEW);

        assertThat(result.saved()).isEqualTo(1);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0)).contains("Network error");

        verify(storage).save(anyList(), any());
    }

    private AggregatedItem createItem(String source) {
        return new AggregatedItem(source, "title", Map.of(), Instant.now());
    }
}
