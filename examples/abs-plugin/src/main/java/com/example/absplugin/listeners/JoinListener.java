package com.example.absplugin.listeners;

import dev.j2kt.pumpkin.abs.EventHandler;
import pumpkin.Event;
import pumpkin.EventPriority;
import pumpkin.EventType;
import pumpkin.TextComponent;

public final class JoinListener {
    @EventHandler(type = EventType.PLAYER_JOIN_EVENT, priority = EventPriority.NORMAL)
    public void onJoin(Event.PlayerJoinEvent event) {
        event.getPlayer().sendMessage(TextComponent.text("Welcome"));
    }
}
