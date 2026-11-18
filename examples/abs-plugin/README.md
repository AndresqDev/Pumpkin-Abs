# Pumpkin Abs Example

Bukkit-style example using `PumpkinAbsPlugin`, a separate `CommandProvider` command, an `@EventHandler` listener, and the generated command/listener bootstrap.

## Generate listener registration

From the Pumpkin Abs repository root:

```bash
java -jar build/libs/pumpkin-abs-tools-0.1.1.jar \
  --source examples/abs-plugin/src/main/java \
  --package com.example.absplugin \
  --output examples/abs-plugin/src/generated/java \
  --main com.example.absplugin.AbsExamplePlugin
```

## Compile Java

```bash
javac --release 21 \
  -cp build/libs/pumpkin-abs-0.1.1.jar:libs/pumpkin-stub.jar \
  -d examples/abs-plugin/build \
  $(find examples/abs-plugin/src -name '*.java')
```

## J2KT translation

Include the Pumpkin Abs sources, the example sources, and the generated sources:

```bash
java -jar j2kt-pumpkin-0.1.1-all.jar \
  --target pumpkin \
  --classpath libs/pumpkin-stub.jar \
  --out build/translated \
  src/main/java \
  examples/abs-plugin/src/main/java \
  examples/abs-plugin/src/generated/java
```

Set the Pumpkin plugin class to the generated bootstrap:

```kotlin
pumpkin {
    pluginClass.set("com.example.absplugin.generated.GeneratedPlugin")
}
```

The generated plugin registers every `@EventHandler` method found in the listener source tree.
