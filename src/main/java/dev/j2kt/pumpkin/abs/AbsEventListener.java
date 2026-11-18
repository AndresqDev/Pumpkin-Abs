package dev.j2kt.pumpkin.abs;

import pumpkin.Event;
import pumpkin.Server;

/** Event callback used by the Bukkit-style base plugin. */
@FunctionalInterface
public interface AbsEventListener {
    Event handle(Server server, Event event);
}
