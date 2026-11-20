package com.aggregator.cli;

import com.aggregator.core.aggregation.Aggregator;
import com.aggregator.core.storage.Storage;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CliTest {
    @Test
    void shouldReturnErrorCodeOnUnknownFormat() {
        Aggregator aggregator = mock(Aggregator.class);
        Storage storage = mock(Storage.class);
        Cli cli = new Cli(aggregator, Map.of("json", storage), new ItemPrinter());
        int exitCode = cli.run(new String[]{"poll", "--format", "xml"});
        assertThat(exitCode).isEqualTo(1);
        verifyNoInteractions(aggregator);
    }

    @Test
    void shouldReturnErrorCodeOnMissingValue() {
        Aggregator aggregator = mock(Aggregator.class);
        Cli cli = new Cli(aggregator, Map.of(), new ItemPrinter());
        int exitCode = cli.run(new String[]{"poll", "--sources"});
        assertThat(exitCode).isEqualTo(1);
        verifyNoInteractions(aggregator);
    }
}
