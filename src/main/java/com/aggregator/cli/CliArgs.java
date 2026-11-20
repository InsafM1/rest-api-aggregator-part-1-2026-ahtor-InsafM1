package com.aggregator.cli;

import com.aggregator.core.storage.WriteMode;
import java.util.List;

public record CliArgs(
        List<String> sources,
        String format,
        WriteMode mode,
        String sourceFilter
) {}