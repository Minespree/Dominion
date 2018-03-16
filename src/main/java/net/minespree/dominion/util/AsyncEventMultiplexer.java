package net.minespree.dominion.util;

import net.md_5.bungee.api.event.AsyncEvent;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Provides a multiplexer for {@link AsyncEvent} handlers.
 */
public class AsyncEventMultiplexer<E extends AsyncEvent> {
    private final Plugin plugin;
    private final List<MultiplexHandler<E>> listeners = new ArrayList<>();

    public AsyncEventMultiplexer(Plugin plugin) {
        this.plugin = plugin;
    }

    public void addListener(MultiplexHandler<E> listener) {
        listeners.add(listener);
    }

    public void handle(E event) {
        event.registerIntent(plugin);
        CompletableFuture[] futures = listeners.stream().map(h -> h.handle(event)).toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(futures).whenComplete((ignored, ex) -> {
            event.completeIntent(plugin);
            if (ex != null) {
                plugin.getLogger().log(Level.SEVERE, "Error while processing " + event, ex);
            }
        });
    }
}
