package de.jxdev.legendarycraft.v3.event;

@FunctionalInterface
public interface EventListener<T extends Event> {
    void onEvent(T event);
}
