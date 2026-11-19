# Pumpkin Abs

Bukkit-style abstraction for Pumpkin plugins. Pumpkin Abs sits between the Pumpkin Java stub and plugin code and provides lifecycle hooks, command registration, listener registration, permission handling, and event subscription cleanup.

Metadata stays in `metadata()`. Plugin code registers commands and listeners in `onEnable()` instead of the raw Pumpkin lifecycle.

Pumpkin Abs does not reimplement Pumpkin Stub. It adds the plugin bootstrap and registration layer; gameplay types such as players, worlds, inventories, GUIs, events, packets, and tasks are still used directly from the stub. See [COVERAGE.md](COVERAGE.md) for the exact boundary and current limitations.

## Compatibility

| Component | Version |
|---|---|
| Pumpkin Abs | 0.2.0 |
| Java | 21 |
| Pumpkin Stub | 0.1.1 |
| J2KT Pumpkin | 0.1.3 |
| Pumpkin Kotlin/Wasm API | 0.1.2 |
| Pumpkin plugin WIT | `pumpkin:plugin@0.1.0` (synchronous) |

Pumpkin Abs targets the synchronous v0.1 WIT ABI.

## Installation

Download the release JARs from GitHub:

```text
pumpkin-abs-0.2.0.jar
pumpkin-abs-0.2.0-sources.jar
```

Compile plugin Java against `pumpkin-abs-0.2.0.jar` and `pumpkin-stub-0.1.1.jar`.

For J2KT translation, the Pumpkin Abs source must be included in the translated source set. A plugin using the precompiled JAR at runtime needs the translated Abs sources in the Wasm build:

```text
pumpkin-abs/src/main/java
pumpkin-abs/src/generated/java
```

The release `pumpkin-abs-0.2.0-sources.jar` contains those sources. The J2KT-generated plugin also needs the `pumpkin-compat/src/wasmWasiMain/kotlin` source set, because the Abs base class uses its command and event registries.

## Quick start

```java
package com.example.plugin;

import java.util.List;

import dev.j2kt.pumpkin.abs.PumpkinAbsPlugin;
import pumpkin.PluginMetadata;

public class ExamplePlugin extends PumpkinAbsPlugin {
    @Override
    public PluginMetadata metadata() {
        return new PluginMetadata(
                "example-plugin",
                "0.1.0",
                List.of("You"),
                "Example Pumpkin Abs plugin",
                List.of(),
                List.of());
    }

    @Override
    protected void onEnable() {
    }
}
```

`onEnable` runs after the Pumpkin plugin context is available. `onDisable` is optional and runs before the base class unregisters event subscriptions.

## Stub access and generated facade

Abs does not duplicate the Pumpkin Stub type hierarchy. The re-export strategy is intentional: gameplay objects remain the original `pumpkin.*` types, so they stay compatible with Stub, J2KT, and generated code.

The base class exposes common entry points directly:

```java
Player player = getPlayer("Alex");
World world = getWorld("world");
Iterable<Player> players = getOnlinePlayers();
Iterable<World> worlds = getWorlds();

broadcast("Server message");
double tps = getTps();
double mspt = getMspt();

EventRegistry events = events();
CommandRegistry commands = commands();
TaskScheduler tasks = scheduler();
IpcRegistry ipc = ipc();
AiGoalRegistry aiGoals = aiGoals();
```

`getOnlinePlayers` and `getWorlds` return `Iterable` so the same source compiles in Java and Kotlin/Wasm. Use `getServer().getAllPlayers()` and `getServer().getAllWorlds()` when a concrete `List` is required.

The `dev.j2kt.pumpkin.abs.reexport.AbsApi` class is generated from the Stub source and exposes the same resource registries:

```java
AbsApi.events(this);
AbsApi.commands(this);
AbsApi.scheduler(this);
AbsApi.ipc(this);
AbsApi.aiGoals(this);
```

Regenerate it when Stub changes:

```bash
./gradlew generateReexports -PstubSource=../pumpkin-stub/src/main/java
```

The task reads `pumpkin-stub/src/main/java/pumpkin/PumpkinPlugin.java` and writes `src/generated/java`. The generated source is part of the Abs artifact and is included in the J2KT translation input.

The same generator is available in the tools JAR:

```bash
java -cp build/libs/pumpkin-abs-tools-0.2.0.jar \
  dev.j2kt.pumpkin.abs.generator.ReexportGenerator \
  --stub-source ../pumpkin-stub/src/main/java \
  --output src/generated/java
```

For APIs outside the Abs bootstrap, use the Stub types directly. They are not hidden behind a second wrapper hierarchy.

## Registration from the plugin main

The base class exposes build-time registration markers and runtime registration methods.

```java
@Override
protected void onEnable() {
    registerListeners("mylisteners");
    registerListeners("mylisteners/sectionone");
    registerCommands("mycommands");
}
```

The marker methods are read by `pumpkin-abs-tools` during the build. They do not scan packages at runtime because Pumpkin/Wasm does not provide reflection.

Paths are resolved relative to the configured source root and the plugin package. Absolute paths and source-file paths are also accepted. Multiple paths may be passed to either method.

The generator reads these markers when `--main` resolves the main source file or when `--main-source` is supplied. The marker methods are no-ops at runtime; the generated plugin performs the registration.

For individual objects, use the runtime methods:

```java
registerCommand(new PingCommand());
registerListener(new ManualJoinListener());
```

`registerListener` requires a `Listener` implementation with a `register(PumpkinAbsPlugin)` method. For an annotated `@EventHandler` listener, either include its file or folder in a marker, or use the generated listener registry.

Do not use both generated registration and manual registration for the same command or listener.

## Commands

A command lives in its own file under `commands/` and extends `AbsCommand`. The command class contains only its metadata and execution logic; it does not construct `PluginCommand`.

```java
package com.example.plugin.commands;

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
        return "Returns pong";
    }

    @Override
    public boolean onCommand(Command.CommandSender sender, String label, List<String> args) {
        String response = args.isEmpty() ? "pong" : "pong " + args.get(0);
        sender.sendMessage(TextComponent.text(response));
        return true;
    }
}
```

`GeneratedCommands` detects `AbsCommand` subclasses and `CommandProvider` implementations. No command registration code is needed in the main plugin.

Commands can also be registered manually:

```java
registerCommand(new PingCommand());
```

`PluginCommand` remains available for commands that need to configure aliases, permissions, or executors directly.

`getCommand("ping")` returns a registered command by name or alias. Register the permission node in metadata and with `registerPermission` when using an explicit permission.

Command arguments are passed to `onCommand` as a whitespace-separated list. The command is registered with a greedy argument node, so `/command value` and `/command` both reach the same executor.

## Listeners

A listener lives in its own file under `listeners/` and uses `@EventHandler`:

```java
package com.example.plugin.listeners;

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
```

### Build-time listener generation

Pumpkin/Wasm does not provide runtime class or package reflection. Listener discovery therefore runs at build time.

Generate command and listener registration from the source tree:

```bash
java -jar pumpkin-abs-tools-0.2.0.jar \
  --source src/main/java \
  --package com.example.plugin \
  --output build/generated/abs \
  --main com.example.plugin.ExamplePlugin
```

Custom folders are supported with repeatable `--listeners` and `--commands` options. Each option accepts a directory or an individual Java file:

```bash
java -jar pumpkin-abs-tools-0.2.0.jar \
  --listeners src/main/java/com/example/plugin/mylisteners \
  --commands src/main/java/com/example/plugin/mycommands \
  --package com.example.plugin \
  --output build/generated/abs \
  --main com.example.plugin.ExamplePlugin
```

To register a single listener or command file, pass that file path to the corresponding option.

If the main source file cannot be derived from `--main`, pass it explicitly with `--main-source`. The generator reads any `registerListeners` and `registerCommands` markers from that file.

The generator writes:

```text
build/generated/abs/com/example/plugin/generated/GeneratedListeners.java
build/generated/abs/com/example/plugin/generated/GeneratedCommands.java
build/generated/abs/com/example/plugin/generated/GeneratedPlugin.java
```

`GeneratedPlugin` extends the plugin main class, registers generated commands, registers generated listeners, and becomes the build's plugin class:

```kotlin
pumpkin {
    pluginClass.set("com.example.plugin.generated.GeneratedPlugin")
}
```

The main plugin class must not be `final`.

Add `build/generated/abs` to the translated Java source set together with the Pumpkin Abs sources, `pumpkin-abs/src/generated/java`, and the plugin sources.

Without generated discovery, implement `Listener` and register it manually:

```java
public final class ManualJoinListener implements Listener {
    @Override
    public void register(PumpkinAbsPlugin plugin) {
        plugin.listen(
                EventType.PLAYER_JOIN_EVENT,
                EventPriority.NORMAL,
                false,
                (server, event) -> {
                    Event.PlayerJoinEvent join = (Event.PlayerJoinEvent) event;
                    join.getPlayer().sendMessage(TextComponent.text("Welcome"));
                    return event;
                });
    }
}
```

```java
registerListener(new ManualJoinListener());
```

## Permissions

Use the Pumpkin metadata namespace:

```text
<plugin-name>:<permission>
```

Commands without an explicit permission receive:

```text
<plugin-name>:command.<command-name>
```

with default `ALLOW`.

## Build

```bash
./gradlew clean build toolsJar
```

Regenerate the Stub facade when the Stub source changes:

```bash
./gradlew generateReexports -PstubSource=../pumpkin-stub/src/main/java
```

Artifacts:

```text
build/libs/pumpkin-abs-0.2.0.jar
build/libs/pumpkin-abs-0.2.0-sources.jar
build/libs/pumpkin-abs-tools-0.2.0.jar
```

## Releases

The workflow in `.github/workflows/release.yml` runs on tags matching `v*`. It builds the library and tools JARs, uploads them as a GitHub Actions artifact, downloads the same artifact in the release job, and publishes the JARs with SHA-256 checksums.

Publish with:

```bash
git tag v0.2.0
git push origin v0.2.0
```

## Limitations

- Runtime reflection and runtime package scanning are unavailable in Pumpkin/Wasm. Listener discovery is a build-time step.
- Gameplay types remain Stub types. Abs does not provide a second wrapper hierarchy such as `AbsPlayer` or `AbsWorld`.
- `getOnlinePlayers` and `getWorlds` return `Iterable`; use `getServer()` for `List` access.
- `AbsApi` must be regenerated when the Stub source changes.
- Command arguments are split on whitespace; quoted argument grouping is not implemented.
- Explicit command permissions must be registered by the plugin.
- Listener classes instantiated by generated code need public no-argument constructors and public event methods.
- Command classes need public no-argument constructors.
- The Pumpkin Abs source and generated facade source must be included in the J2KT translation input; the precompiled JAR is not embedded into the Wasm component.
- World generation is not part of this library.

## Source

```text
https://github.com/AndresqDev/Pumpkin-Abs
```

## License

Apache License 2.0. See `LICENSE`.
