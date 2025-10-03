package de.jxdev.legendarycraft.v3.event;

import java.util.*;

public class EventDispatcher {
    private final Map<Class<? extends Event>, List<EventListener<?>>> listeners = new HashMap<>();

    public synchronized <T extends Event> void registerListener(Class<T> eventType, EventListener<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    }

    @SuppressWarnings("unchecked")
    public <T extends Event> void dispatchEvent(T event) {
        if (event == null) return;
        Class<? extends Event> type = event.getClass();
        List<EventListener<?>> ls = listeners.get(type);
        if (ls == null || ls.isEmpty()) return;
        // iterate over a snapshot to avoid concurrent modification if listeners add/remove during dispatch
        for (EventListener<?> l : new ArrayList<>(ls)) {
            ((EventListener<T>) l).onEvent(event);
        }
    }
}
