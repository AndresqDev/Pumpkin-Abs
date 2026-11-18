package dev.j2kt.pumpkin.abs;

/** Supplies a command definition without extending PluginCommand. */
@FunctionalInterface
public interface CommandProvider {
    PluginCommand command();
}
