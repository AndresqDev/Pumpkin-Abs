package com.example.absplugin.commands;

import java.util.List;

import dev.j2kt.pumpkin.abs.AbsCommand;
import pumpkin.Command;
import pumpkin.TextComponent;

public final class PingCommand extends AbsCommand {
    @Override
    public String name() {
        return "ping";
    }

    @Override
    public String description() {
        return "Returns pong and echoes arguments";
    }

    @Override
    public boolean onCommand(Command.CommandSender sender, String label, List<String> args) {
        String response = "pong";
        for (String arg : args) {
            response = response + " " + arg;
        }
        sender.sendMessage(TextComponent.text(response));
        return true;
    }
}
