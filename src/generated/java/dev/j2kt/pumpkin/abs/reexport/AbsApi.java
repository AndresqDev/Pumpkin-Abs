package dev.j2kt.pumpkin.abs.reexport;

import dev.j2kt.pumpkin.abs.PumpkinAbsPlugin;
import pumpkin.*;

/** Generated low-level accessors for plugin resources. */
public final class AbsApi {
    public static AiGoalRegistry aiGoals(PumpkinAbsPlugin plugin) {
        return plugin.getAiGoals();
    }

    public static CommandRegistry commands(PumpkinAbsPlugin plugin) {
        return plugin.getCommands();
    }

    public static EventRegistry events(PumpkinAbsPlugin plugin) {
        return plugin.getEvents();
    }

    public static IpcRegistry ipc(PumpkinAbsPlugin plugin) {
        return plugin.getIpc();
    }

    public static TaskScheduler tasks(PumpkinAbsPlugin plugin) {
        return plugin.getTasks();
    }

}
