package com.aggregator;

import com.aggregator.cli.Cli;
import com.aggregator.cli.ItemPrinter;
import com.aggregator.config.AppConfig;
import com.aggregator.core.aggregation.Aggregator;
import com.aggregator.core.http.HttpClientWrapper;
import com.aggregator.core.http.JavaHttpClientWrapper;
import com.aggregator.core.source.ApiSource;
import com.aggregator.core.source.frankfurter.FrankfurterSource;
import com.aggregator.core.source.jsonplaceholder.JsonPlaceholderSource;
import com.aggregator.core.source.openmeteo.OpenMeteoSource;
import com.aggregator.core.storage.CsvStorage;
import com.aggregator.core.storage.JsonStorage;
import com.aggregator.core.storage.Storage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.List;
import java.util.Map;

public class AppFactory {
    private AppFactory() {}

    public static Cli createCli() {
        HttpClientWrapper http = new JavaHttpClientWrapper(AppConfig.HTTP_TIMEOUT);

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());

        List<ApiSource> sources = List.of(
                new OpenMeteoSource(http, mapper),
                new FrankfurterSource(http, mapper),
                new JsonPlaceholderSource(http, mapper)
        );

        Storage jsonStorage = new JsonStorage(AppConfig.JSON_FILE, mapper);
        Storage csvStorage  = new CsvStorage(AppConfig.CSV_FILE, mapper);
        Map<String, Storage> storagesByFormat = Map.of(
                "json", jsonStorage,
                "csv",  csvStorage
        );

        Aggregator aggregator = new Aggregator(sources);

        ItemPrinter printer = new ItemPrinter();
        return new Cli(aggregator, storagesByFormat, printer);
    }
}
