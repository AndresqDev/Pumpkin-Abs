package dev.j2kt.pumpkin.abs.generator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates the low-level Abs re-export facade from the Pumpkin stub source.
 *
 * <p>The facade exposes the plugin resource registries as static accessors.
 * Gameplay APIs remain the original {@code pumpkin.*} types, so code can mix
 * the Abs facade and the stub without adapters or copies.</p>
 */
public final class ReexportGenerator {
    private static final Pattern NO_ARG_METHOD = Pattern.compile(
            "\\bpublic\\s+([\\w.$<>\\[\\],? ]+?)\\s+(\\w+)\\s*\\(\\s*\\)\\s*\\{");

    private ReexportGenerator() {
    }

    public static void main(String[] args) throws IOException {
        Path stubSource = null;
        Path output = null;
        String basePackage = "dev.j2kt.pumpkin.abs.reexport";

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--stub-source")) {
                stubSource = Path.of(args[++i]);
            } else if (arg.equals("--output")) {
                output = Path.of(args[++i]);
            } else if (arg.equals("--package")) {
                basePackage = args[++i];
            } else {
                throw new IllegalArgumentException("Unknown argument: " + arg);
            }
        }

        if (stubSource == null || output == null) {
            throw new IllegalArgumentException(
                    "Usage: ReexportGenerator --stub-source <pumpkin-stub/src/main/java> --output <dir> [--package <pkg>]");
        }

        Path pluginSource = stubSource.resolve("pumpkin/PumpkinPlugin.java");
        if (!Files.isRegularFile(pluginSource)) {
            throw new IllegalArgumentException("Missing PumpkinPlugin.java under " + stubSource);
        }

        String source = Files.readString(pluginSource, StandardCharsets.UTF_8);
        Set<Accessor> accessors = new LinkedHashSet<>();

        Matcher matcher = NO_ARG_METHOD.matcher(source);
        while (matcher.find()) {
            String name = matcher.group(2);
            if (!name.startsWith("get") || name.length() <= 3) {
                continue;
            }
            String property = Character.toLowerCase(name.charAt(3)) + name.substring(4);
            if (property.equals("class")) {
                continue;
            }
            accessors.add(new Accessor(property, matcher.group(1).trim()));
        }

        Path packageRoot = output.resolve(basePackage.replace('.', '/'));
        Files.createDirectories(packageRoot);
        StringBuilder out = new StringBuilder();
        out.append("package ").append(basePackage).append(";\n\n");
        out.append("import dev.j2kt.pumpkin.abs.PumpkinAbsPlugin;\n");
        out.append("import pumpkin.*;\n\n");
        out.append("/** Generated low-level accessors for plugin resources. */\n");
        out.append("public final class AbsApi {\n");
        for (Accessor accessor : accessors.stream().sorted(Comparator.comparing(Accessor::name)).toList()) {
            out.append("    public static ").append(accessor.type).append(" ").append(accessor.name)
                    .append("(PumpkinAbsPlugin plugin) {\n");
            out.append("        return plugin.").append("get")
                    .append(Character.toUpperCase(accessor.name.charAt(0)))
                    .append(accessor.name.substring(1)).append("();\n");
            out.append("    }\n\n");
        }
        out.append("}\n");
        Files.writeString(packageRoot.resolve("AbsApi.java"), out.toString(), StandardCharsets.UTF_8);
        System.out.println("Generated " + accessors.size() + " accessor(s) in " + packageRoot);
    }

    private record Accessor(String name, String type) {
    }
}
