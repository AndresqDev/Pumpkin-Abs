# Pumpkin Abs and Pumpkin Stub

Pumpkin Abs is a lifecycle, registration, and access layer over Pumpkin Stub. It does not reimplement the Stub type hierarchy. Functional parity is provided by keeping every Stub API reachable, not by duplicating 46 top-level types in a second wrapper package.

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

## Re-export layer

Abs intentionally re-exports the original `pumpkin.*` types. They remain the single source of truth and are compatible with Stub, J2KT, generated code, and plugin code.

`PumpkinAbsPlugin` exposes the common plugin entry points:

```text
getServer()
getPluginContext()
getDataFolder()
getPlayer(String)
getOnlinePlayers()          // Iterable<Player>
getWorld(String)
getWorlds()                 // Iterable<World>
broadcast(String)
broadcast(TextComponent)
getTps()
getMspt()
events()
commands()
scheduler()
ipc()
aiGoals()
```

The resource registries are also available through the generated `AbsApi` class:

```text
AbsApi.events(plugin)
AbsApi.commands(plugin)
AbsApi.scheduler(plugin)
AbsApi.ipc(plugin)
AbsApi.aiGoals(plugin)
```

`AbsApi` is generated from `pumpkin-stub/src/main/java/pumpkin/PumpkinPlugin.java` by `ReexportGenerator`. The source is committed under `src/generated/java` so the Abs release artifact does not need the Stub source at runtime. Re-run `./gradlew generateReexports -PstubSource=../pumpkin-stub/src/main/java` when Stub changes.

## What remains direct Pumpkin Stub

Plugin code continues to use the original types for:

- `Server`, `Player`, `JavaPlayer`, `BedrockPlayer`, `World`, `BlockPos`, and positions.
- Inventories, items, GUIs, screens, scoreboards, teams, boss bars, and display entities.
- Events and event data beyond the listener registration hook.
- Forms, Java dialogs, recipes, datapacks, enchantments, blocks, mobs, and AI goals.
- Java and Bedrock packets, custom payloads, and IPC.
- Scheduled tasks and task actions.
- Text components and logging.

This is deliberate. A second wrapper hierarchy would duplicate the Stub surface and would require a separate J2KT mapping for every wrapper method. The hybrid model gives Abs a high-level bootstrap while keeping the re-exported layer identical to the API that J2KT already translates.

## Surface comparison

Abs contains eight hand-written public classes:

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

It also contains the generated `dev.j2kt.pumpkin.abs.reexport.AbsApi` accessor class.

Pumpkin Stub contains 46 top-level source files. The class count is not a coverage percentage: Abs adds behavior to the bootstrap and leaves gameplay/data APIs on their original types. A plugin using Abs still compiles Java against Pumpkin Stub and still needs J2KT and its compatibility source set for the Wasm build.

## Is Abs viable for plugin development?

Yes, for plugins that need lifecycle, commands, listener registration, permissions, event cleanup, and access to the full Pumpkin API. Abs removes the raw registration boilerplate without removing capability; the Stub types remain available.

It is not a complete Bukkit/Spigot replacement. The current limitations are:

- There is no runtime reflection or package scanning. Folder and individual annotated-listener discovery is resolved at build time.
- `registerListener(Listener)` requires a `Listener` implementation with an explicit registration hook. An annotated `@EventHandler` class is registered through the generated listener registry or a marker path.
- Command arguments are whitespace-separated raw values. Typed argument nodes, quoting, and rich suggestions are not implemented by the Abs bridge.
- Abs has no configuration API, no scheduler wrapper, and no inventory/GUI wrappers.
- Gameplay types remain `pumpkin.*` types; there are no `AbsPlayer`/`AbsWorld` wrappers.
- `getOnlinePlayers` and `getWorlds` return `Iterable`. Use `getServer().getAllPlayers()` or `getServer().getAllWorlds()` when a concrete `List` is required.
- The generated plugin class must be configured as the Pumpkin plugin class; the main class cannot be `final`.
- The Abs source, `src/generated/java`, and the J2KT compatibility sources must be included in the Wasm translation input.
- The target is the synchronous `pumpkin:plugin@0.1.0` ABI. The async `0.2.0` ABI is unsupported.
- J2KT translation coverage remains the limiting factor for individual Stub methods.

The practical model is: Stub defines the Pumpkin API, J2KT translates it to Kotlin/Wasm, Abs adds a Bukkit-like bootstrap, and Abs re-exports the Stub entry points without hiding the original types.
