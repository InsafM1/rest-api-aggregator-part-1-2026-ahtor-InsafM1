package com.aggregator.config;

import java.nio.file.Path;
import java.time.Duration;

public final class AppConfig {
    private AppConfig() {}

    public static final Duration HTTP_TIMEOUT = Duration.ofSeconds(10);
    public static final Path JSON_FILE = Path.of("data/result.json");
    public static final Path CSV_FILE = Path.of("data/result.csv");
}
