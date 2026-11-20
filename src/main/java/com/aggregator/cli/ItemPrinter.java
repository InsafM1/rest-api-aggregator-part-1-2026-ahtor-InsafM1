package com.aggregator.cli;

import com.aggregator.core.model.AggregatedItem;
import java.util.List;

public class ItemPrinter {
    public void print(List<AggregatedItem> items) {
        if (items.isEmpty()) {
            System.out.println("(no records)");
            return;
        }
        for (AggregatedItem item : items) {
            System.out.println("[" + item.source() + "] " + item.title()
                    + " @ " + item.fetchedAt());
            item.attributes().forEach((k, v) ->
                    System.out.println("    " + k + " = " + v));
            System.out.println();
        }
        System.out.println("Total: " + items.size());
    }
}
