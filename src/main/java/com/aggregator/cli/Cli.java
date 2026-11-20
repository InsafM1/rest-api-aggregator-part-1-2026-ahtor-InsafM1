package com.aggregator.cli;

import com.aggregator.core.aggregation.AggregationResult;
import com.aggregator.core.aggregation.Aggregator;
import com.aggregator.core.model.AggregatedItem;
import com.aggregator.core.storage.Storage;
import com.aggregator.core.storage.WriteMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Cli {
    private static final Logger log = LoggerFactory.getLogger(Cli.class);

    private final Aggregator aggregator;
    private final Map<String, Storage> storagesByFormat;
    private final ItemPrinter printer;

    public Cli(Aggregator aggregator,
               Map<String, Storage> storagesByFormat,
               ItemPrinter printer) {
        this.aggregator = aggregator;
        this.storagesByFormat = storagesByFormat;
        this.printer = printer;
    }

    public int run(String[] args) {
        if (args.length == 0) {
            printUsage();
            return 1;
        }
        String command = args[0];
        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        return switch (command) {
            case "poll" -> runPoll(rest);
            case "show" -> runShow(rest);
            case "help", "--help", "-h" -> { printUsage(); yield 0; }
            default -> {
                System.err.println("Unknown command: " + command);
                printUsage();
                yield 1;
            }
        };
    }

    private int runPoll(String[] args) {
        CliArgs parsed;
        try {
            parsed = parseArgs(args);
        } catch (IllegalArgumentException e) {
            log.error("Invalid arguments: {}", e.getMessage());
            System.err.println("Error: " + e.getMessage());
            return 1;
        }

        Storage storage = storagesByFormat.get(parsed.format());
        if (storage == null) {
            System.err.println("Unknown format: " + parsed.format()
                    + " (available: " + storagesByFormat.keySet() + ")");
            return 1;
        }

        Map<String, Map<String, String>> paramsBySource = Map.of();
        try {
            AggregationResult result = aggregator.poll(
                    parsed.sources(),
                    paramsBySource,
                    storage,
                    parsed.mode() == null ? WriteMode.NEW : parsed.mode()
            );
            System.out.println("Saved " + result.saved() + " items, "
                    + result.errors().size() + " errors");
            result.errors().forEach(e -> System.out.println("  - " + e));
            return 0;
        } catch (Exception e) {
            log.error("Polling failed", e);
            System.err.println("Polling failed: " + e.getMessage());
            return 2;
        }
    }

    private int runShow(String[] args) {
        CliArgs parsed;
        try {
            parsed = parseArgs(args);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }

        Storage storage = storagesByFormat.get(parsed.format());
        if (storage == null) {
            System.err.println("Unknown format: " + parsed.format()
                    + " (available: " + storagesByFormat.keySet() + ")");
            return 1;
        }

        try {
            List<AggregatedItem> items = (parsed.sourceFilter() == null)
                    ? storage.load()
                    : storage.loadBySource(parsed.sourceFilter());
            printer.print(items);
            return 0;
        } catch (Exception e) {
            log.error("Show failed", e);
            System.err.println("Show failed: " + e.getMessage());
            return 2;
        }
    }

    private CliArgs parseArgs(String[] args) {
        List<String> sources = List.of();
        String format = "json";
        WriteMode mode = null;
        String sourceFilter = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--sources" -> {
                    String value = requireValue(args, ++i, "--sources");
                    sources = Arrays.stream(value.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toList();
                }
                case "--format" -> {
                    format = requireValue(args, ++i, "--format");
                    if (!format.equals("json") && !format.equals("csv")) {
                        throw new IllegalArgumentException(
                                "format must be json or csv, got: " + format);
                    }
                }
                case "--mode" -> {
                    String value = requireValue(args, ++i, "--mode");
                    mode = switch (value) {
                        case "new" -> WriteMode.NEW;
                        case "append" -> WriteMode.APPEND;
                        default -> throw new IllegalArgumentException(
                                "mode must be new or append, got: " + value);
                    };
                }
                case "--source" -> sourceFilter = requireValue(args, ++i, "--source");
                default -> throw new IllegalArgumentException("Unknown flag: " + arg);
            }
        }
        return new CliArgs(sources, format, mode, sourceFilter);
    }

    private String requireValue(String[] args, int index, String flag) {
        if (index >= args.length) {
            throw new IllegalArgumentException(flag + " requires a value");
        }
        String value = args[index];
        if (value.startsWith("--")) {
            throw new IllegalArgumentException(
                    flag + " requires a value, got flag: " + value);
        }

        return value;
    }

    private void printUsage() {
        System.out.println("""
                Usage:
                  poll --sources a,b,c --format json|csv --mode new|append
                  show --format json|csv [--source name]
                  help
                """);
    }
}
