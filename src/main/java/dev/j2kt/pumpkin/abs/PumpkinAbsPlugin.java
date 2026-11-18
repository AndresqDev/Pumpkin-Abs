package dev.j2kt.pumpkin.abs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pumpkin.Command;
import pumpkin.EventPriority;
import pumpkin.EventSubscription;
import pumpkin.EventType;
import pumpkin.Permission;
import pumpkin.PluginContext;
import pumpkin.PumpkinPlugin;
import pumpkin.Server;

/**
 * Base class for Bukkit-style Pumpkin plugins.
 *
 * Subclasses implement metadata and register commands and listeners in onEnable.
 * The base class owns the plugin context, command lookup, listener registration,
 * and event subscription cleanup.
 */
public abstract class PumpkinAbsPlugin extends PumpkinPlugin {
    private PluginContext pluginContext;
    private Server server;
    private final Map<String, PluginCommand> registeredCommands = new HashMap<>();
    private final List<EventSubscription> eventSubscriptions = new ArrayList<>();
    private final Set<String> registeredPermissions = new HashSet<>();

    @Override
    public final void onLoad(PluginContext context) {
        this.pluginContext = context;
        this.server = context.getServer();
        onEnable();
    }

    @Override
    public final void onUnload(PluginContext context) {
        onDisable();
        for (EventSubscription subscription : eventSubscriptions) {
            subscription.unregister();
        }
        eventSubscriptions.clear();
        registeredCommands.clear();
        registeredPermissions.clear();
    }

    protected abstract void onEnable();

    protected void onDisable() {
    }

    public final PluginContext getPluginContext() {
        return requireContext();
    }

    public final Server getServer() {
        return server;
    }

    public final String getDataFolder() {
        return requireContext().getDataFolder();
    }

    public final String getPluginName() {
        return metadata().getName();
    }

    public final PluginCommand registerCommand(PluginCommand command) {
        PluginContext context = requireContext();
        String permission = command.permission == null || command.permission.isEmpty()
                ? getPluginName() + ":command." + command.name
                : command.permission;

        if (command.permission == null || command.permission.isEmpty()) {
            registerPermission(permission, command.description, Permission.PermissionDefault.ALLOW);
        }

        List<String> names = new ArrayList<>();
        names.add(command.name);
        names.addAll(command.aliases);

        getCommands().register(
                context,
                new Command(names, command.description),
                permission,
                (sender, srv, args) -> command.execute(sender, command.name, new ArrayList<>()) ? 1 : 0);

        registeredCommands.put(command.name, command);
        for (String alias : command.aliases) {
            registeredCommands.put(alias, command);
        }
        return command;
    }

    public final PluginCommand registerCommand(AbsCommand command) {
        PluginCommand wrapped = new PluginCommand(command.name(), command.description());
        for (String alias : command.aliases()) {
            wrapped.alias(alias);
        }
        wrapped.permission(command.permission());
        wrapped.executor((sender, cmd, label, args) -> command.onCommand(sender, label, args));
        return registerCommand(wrapped);
    }

    public final PluginCommand getCommand(String name) {
        PluginCommand command = registeredCommands.get(name);
        if (command == null) {
            throw new IllegalArgumentException("Unknown command: " + name);
        }
        return command;
    }

    public final void registerEvents(Listener listener) {
        listener.register(this);
    }

    public final EventSubscription listen(
            EventType type,
            EventPriority priority,
            boolean blocking,
            AbsEventListener listener) {
        EventSubscription subscription = getEvents().listen(
                requireContext(),
                type,
                priority,
                blocking,
                (server, event) -> listener.handle(server, event));
        eventSubscriptions.add(subscription);
        return subscription;
    }

    public final void registerPermission(
            String node,
            String description,
            Permission.PermissionDefault defaultValue) {
        if (registeredPermissions.add(node)) {
            requireContext().registerPermission(new Permission(node, description, defaultValue, List.of()));
        }
    }

    private PluginContext requireContext() {
        if (pluginContext == null) {
            throw new IllegalStateException("PumpkinAbsPlugin is not loaded");
        }
        return pluginContext;
    }
}
