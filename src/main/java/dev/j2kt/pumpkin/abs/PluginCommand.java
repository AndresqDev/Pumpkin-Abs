package dev.j2kt.pumpkin.abs;

import java.util.ArrayList;
import java.util.List;

import pumpkin.Command;

/** Bukkit-style command definition with an executor. */
public class PluginCommand {
    public String name;
    public List<String> aliases = new ArrayList<>();
    public String description;
    public String usage;
    public String permission;
    private CommandExecutor executor = null;

    public PluginCommand(String name, String description) {
        this.name = name;
        this.description = description;
        this.usage = "";
        this.permission = "";
    }

    public PluginCommand alias(String alias) {
        aliases.add(alias);
        return this;
    }

    public PluginCommand permission(String permission) {
        this.permission = permission;
        return this;
    }

    public PluginCommand executor(CommandExecutor executor) {
        this.executor = executor;
        return this;
    }

    public boolean execute(Command.CommandSender sender, String label, List<String> args) {
        CommandExecutor current = executor;
        return current != null && current.onCommand(sender, this, label, args);
    }
}
