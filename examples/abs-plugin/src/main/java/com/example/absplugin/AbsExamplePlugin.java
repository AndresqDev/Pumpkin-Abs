package com.example.absplugin;

import java.util.List;

import dev.j2kt.pumpkin.abs.PumpkinAbsPlugin;
import pumpkin.PluginMetadata;

public class AbsExamplePlugin extends PumpkinAbsPlugin {
    @Override
    public PluginMetadata metadata() {
        return new PluginMetadata(
                "abs-example",
                "0.1.0",
                List.of("Example"),
                "A Bukkit-style Pumpkin plugin example",
                List.of(),
                List.of());
    }

    @Override
    protected void onEnable() {
        registerListeners("listeners");
        registerCommands("commands");
    }
}
