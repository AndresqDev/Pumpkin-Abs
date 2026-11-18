package dev.j2kt.pumpkin.abs;

import java.util.List;

import pumpkin.Command;

/** Command definition with Bukkit-style metadata and execution. */
public abstract class AbsCommand {
    public abstract String name();

    public String description() {
        return "";
    }

    public List<String> aliases() {
        return List.of();
    }

    public String permission() {
        return "";
    }

    public abstract boolean onCommand(Command.CommandSender sender, String label, List<String> args);
}
