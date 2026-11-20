package com.aggregator;

import com.aggregator.cli.Cli;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            Cli cli = AppFactory.createCli();
            int exitCode = cli.run(args);
            System.exit(exitCode);
        } catch (Exception e) {
            log.error("Fatal error", e);
            System.err.println("Fatal error: " + e.getMessage());
            System.exit(2);
        }
    }
}