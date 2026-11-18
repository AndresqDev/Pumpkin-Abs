package com.example.absplugin.commands;

import dev.j2kt.pumpkin.abs.CommandProvider;
import dev.j2kt.pumpkin.abs.PluginCommand;
import pumpkin.TextComponent;

public final class PingCommand implements CommandProvider {
    @Override
    public PluginCommand command() {
        PluginCommand command = new PluginCommand("ping", "Returns pong");
        command.executor((sender, cmd, label, args) -> {
            sender.sendMessage(TextComponent.text("pong"));
            return true;
        });
        return command;
    }
}
