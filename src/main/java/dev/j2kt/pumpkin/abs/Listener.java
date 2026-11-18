package dev.j2kt.pumpkin.abs;

/** Marker and registration hook for plugin listeners. */
public interface Listener {
    void register(PumpkinAbsPlugin plugin);
}
