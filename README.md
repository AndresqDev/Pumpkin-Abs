# Pumpkin Abs

Bukkit-style abstraction for Pumpkin plugins. Pumpkin Abs sits between the Pumpkin Java stub and plugin code and provides lifecycle hooks, command registration, listener registration, permission handling, and event subscription cleanup.

Metadata stays in `metadata()`. Plugin code registers commands and listeners in `onEnable()` instead of the raw Pumpkin lifecycle.

## Compatibility

| Component | Version |
|---|---|
| Pumpkin Abs | 0.1.0 |
| Java | 21 |
| Pumpkin Stub | 0.1.0 |
| J2KT Pumpkin | 0.1.1 |
| Pumpkin Kotlin/Wasm API | 0.1.2 |
| Pumpkin plugin WIT | `pumpkin:plugin@0.1.0` (synchronous) |

Pumpkin Abs targets the synchronous v0.1 WIT ABI.

## Installation

Download the release JARs from GitHub:

```text
pumpkin-abs-0.1.0.jar
pumpkin-abs-0.1.0-sources.jar
```

Compile plugin Java against `pumpkin-abs-0.1.0.jar` and `pumpkin-stub-0.1.0.jar`.

For J2KT translation, the Pumpkin Abs source must be included in the translated source set. A plugin using the precompiled JAR at runtime needs the translated Abs sources in the Wasm build:

```text
pumpkin-abs/src/main/java
```

The release `pumpkin-abs-0.1.0-sources.jar` contains those sources.

## Quick start

```java
package com.example.plugin;

import java.util.List;

import dev.j2kt.pumpkin.abs.PluginCommand;
import dev.j2kt.pumpkin.abs.PumpkinAbsPlugin;
import pumpkin.PluginMetadata;
import pumpkin.TextComponent;

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
        PluginCommand ping = new PluginCommand("ping", "Returns pong");
        ping.executor((sender, command, label, args) -> {
            sender.sendMessage(TextComponent.text("pong"));
            return true;
        });
        registerCommand(ping);
    }
}
```

`onEnable` runs after the Pumpkin plugin context is available. `onDisable` is optional and runs before the base class unregisters event subscriptions.

## Commands

```java
PluginCommand command = new PluginCommand("hello", "Sends a greeting");
command.alias("hi");
command.permission("example-plugin:command.hello");
command.executor((sender, command, label, args) -> {
    sender.sendMessage(TextComponent.text("Hello"));
    return true;
});
registerCommand(command);
```

`getCommand("hello")` returns a registered command by name or alias. Register the permission node in metadata and with `registerPermission` when using an explicit permission.

Command arguments are not yet exposed by the current command stub. The executor receives an empty argument list.

## Listeners

A listener implements `Listener` and registers its event callbacks against the base plugin:

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

Generate listener registration from the plugin listener folder:

```bash
java -jar pumpkin-abs-tools-0.1.0.jar \
  --source src/main/java \
  --package com.example.plugin \
  --output build/generated/abs \
  --main com.example.plugin.ExamplePlugin
```

The generator writes:

```text
build/generated/abs/com/example/plugin/generated/GeneratedListeners.java
build/generated/abs/com/example/plugin/generated/GeneratedPlugin.java
```

`GeneratedPlugin` extends the plugin main class, calls `GeneratedListeners.register(this)` after `onEnable`, and becomes the build's plugin class:

```kotlin
pumpkin {
    pluginClass.set("com.example.plugin.generated.GeneratedPlugin")
}
```

The main plugin class must not be `final`.

Add `build/generated/abs` to the translated Java source set together with the Pumpkin Abs sources and the plugin sources.

Without generated discovery, listeners can be registered manually:

```java
registerEvents(new JoinListener());
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

Artifacts:

```text
build/libs/pumpkin-abs-0.1.0.jar
build/libs/pumpkin-abs-0.1.0-sources.jar
build/libs/pumpkin-abs-tools-0.1.0.jar
```

## Releases

The workflow in `.github/workflows/release.yml` runs on tags matching `v*`. It builds the library and tools JARs, uploads them as a GitHub Actions artifact, downloads the same artifact in the release job, and publishes the JARs with SHA-256 checksums.

Publish with:

```bash
git tag v0.1.0
git push origin v0.1.0
```

## Limitations

- Runtime reflection and runtime package scanning are unavailable in Pumpkin/Wasm. Listener discovery is a build-time step.
- Command arguments are not yet exposed by the current stub bridge.
- Explicit command permissions must be registered by the plugin.
- Listener classes instantiated by generated code need public no-argument constructors and public event methods.
- The Pumpkin Abs source must be included in the J2KT translation input; the precompiled JAR is not embedded into the Wasm component.
- World generation is not part of this library.

## Source

```text
https://github.com/AndresqDev/Pumpkin-Abs
```

## License

Apache License 2.0. See `LICENSE`.
