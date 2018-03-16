package net.minespree.dominion.util;

import net.md_5.bungee.api.event.AsyncEvent;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface MultiplexHandler<E extends AsyncEvent> {
    CompletableFuture<Void> handle(E event);
}
