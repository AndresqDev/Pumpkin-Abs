package dev.j2kt.pumpkin.abs;

import pumpkin.Command;

/** Bukkit-style command executor. */
@FunctionalInterface
public interface CommandExecutor {
    boolean onCommand(Command.CommandSender sender, PluginCommand command, String label, java.util.List<String> args);
}
