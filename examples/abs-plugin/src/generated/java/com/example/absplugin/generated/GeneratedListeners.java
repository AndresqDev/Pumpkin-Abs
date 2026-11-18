package com.example.absplugin.generated;

import dev.j2kt.pumpkin.abs.PumpkinAbsPlugin;
import pumpkin.Event;
import pumpkin.EventPriority;
import pumpkin.EventType;

public final class GeneratedListeners {
    private static final com.example.absplugin.listeners.JoinListener LISTENER_0 = new com.example.absplugin.listeners.JoinListener();

    public static void register(PumpkinAbsPlugin plugin) {
        plugin.listen(EventType.PLAYER_JOIN_EVENT, EventPriority.NORMAL, false,
                (server, event) -> {
                    if (event instanceof Event.PlayerJoinEvent value) {
                        LISTENER_0.onJoin(value);
                    }
                    return event;
                });
    }
}
