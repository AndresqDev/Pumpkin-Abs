# Pumpkin Abs and Pumpkin Stub

Pumpkin Abs is a thin registration and lifecycle layer over Pumpkin Stub. It does not reimplement the stub API and it does not hide Pumpkin's server types behind a complete Bukkit-style API.

## What Abs owns

| Area | Abs responsibility |
|---|---|
| Plugin lifecycle | `PumpkinAbsPlugin` implements `onLoad`/`onUnload`, stores the `PluginContext`, calls `onEnable`/`onDisable`, and clears owned event subscriptions. |
| Metadata | The plugin still implements `metadata()` directly; Abs does not replace `PluginMetadata`. |
| Commands | `AbsCommand` and `PluginCommand` wrap command metadata and execution. The base class registers the command definition, permission, root handler, and greedy raw-argument handler. |
| Command discovery | `pumpkin-abs-tools` scans source files and emits `GeneratedCommands` at build time. |
| Listeners | `Listener`, `@EventHandler`, `GeneratedListeners`, and `listen(...)` handle event subscription. Abs stores and unregisters subscriptions it creates. |
| Listener discovery | `pumpkin-abs-tools` scans annotated methods and emits `GeneratedListeners` at build time. |
| Permissions | `registerPermission` records explicit nodes; default command permissions use the plugin metadata namespace. |

## What remains direct Pumpkin Stub

Abs does not wrap the gameplay and data APIs. Plugin code continues to use the stub types directly for:

- `Server`, `Player`, `JavaPlayer`, `BedrockPlayer`, `World`, `BlockPos`, and positions.
- Inventories, items, GUIs, screens, scoreboards, teams, boss bars, and display entities.
- Events and event data beyond the listener registration hook.
- Forms, Java dialogs, recipes, datapacks, enchantments, blocks, mobs, and AI goals.
- Java and Bedrock packets, custom payloads, and IPC.
- Scheduled tasks and task actions.
- Text components and logging.

There are also areas Abs does not provide at all: configuration files, persistent plugin settings, runtime class scanning, world generation, and a scheduler abstraction.

## Surface comparison

Abs contains eight public classes:

```text
AbsCommand
AbsEventListener
CommandExecutor
CommandProvider
EventHandler
Listener
PluginCommand
PumpkinAbsPlugin
```

Pumpkin Stub contains 46 top-level source files, covering the generated API namespaces. The class count is not a coverage percentage: Abs depends on the stub for nearly every server operation and adds only the plugin bootstrap and registration layer. A plugin using Abs still compiles Java against Pumpkin Stub and still needs the J2KT translator and its compatibility source set for the Wasm build.

## Is Abs viable for plugin development?

Yes, for plugins whose main needs are lifecycle, commands, listener registration, permissions, and event cleanup. It removes the raw registration boilerplate while leaving the full Pumpkin API available through the stub.

No, if the goal is a complete Bukkit/Spigot replacement. The current limitations are material:

- There is no runtime reflection or package scanning. Folder and individual annotated-listener discovery is resolved at build time.
- `registerListener(Listener)` requires a `Listener` implementation with an explicit registration hook. An annotated `@EventHandler` class is registered through the generated listener registry or a marker path.
- Command arguments are whitespace-separated raw values. Typed argument nodes, quoting, and rich suggestions are not implemented by the Abs bridge.
- Abs has no configuration API, no scheduler wrapper, and no inventory/GUI wrappers.
- The generated plugin class must be configured as the Pumpkin plugin class; the main class cannot be `final`.
- The Abs source and the J2KT compatibility sources must be included in the Wasm translation input.
- The target is the synchronous `pumpkin:plugin@0.1.0` ABI. The async `0.2.0` ABI is unsupported.

The practical model is: Stub defines the Pumpkin API, J2KT translates it to Kotlin/Wasm, and Abs adds a Bukkit-like plugin bootstrap on top. Abs is useful as that bootstrap, not as a standalone server API.
