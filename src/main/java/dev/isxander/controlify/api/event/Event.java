package dev.isxander.controlify.api.event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Small array-backed event. Replaces Fabric API's {@code Event} on this Forge port.
 * Listeners registered here run first. The invoker may also post a Forge event.
 */
public final class Event<T> {
    private final List<T> listeners = new ArrayList<>();
    private final Function<List<T>, T> factory;
    private T invoker;

    private Event(Function<List<T>, T> factory) {
        this.factory = factory;
    }

    public static <T> Event<T> of(Function<List<T>, T> factory) {
        return new Event<>(factory);
    }

    public void register(T listener) {
        listeners.add(listener);
        invoker = null;
    }

    public T invoker() {
        if (invoker == null) {
            invoker = factory.apply(List.copyOf(listeners));
        }
        return invoker;
    }
}
