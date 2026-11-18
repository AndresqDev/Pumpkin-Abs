package dev.j2kt.pumpkin.abs.generator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Generates a listener registration class from @EventHandler source methods. */
public final class ListenerGenerator {
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("\\bpackage\\s+([\\w.]+)\\s*;");
    private static final Pattern CLASS_PATTERN = Pattern.compile("\\b(?:public\\s+)?(?:final\\s+)?class\\s+(\\w+)[^\\{]*\\{");
    private static final Pattern COMMAND_PATTERN = Pattern.compile("\\bclass\\s+(\\w+)\\s+implements\\s+(?:[\\w.]+\\.)?CommandProvider\\b");
    private static final Pattern HANDLER_PATTERN = Pattern.compile(
            "@EventHandler\\s*\\(([^)]*)\\)\\s*"
                    + "(?:public|protected|private)?\\s*(?:static\\s+)?(?:final\\s+)?"
                    + "([\\w.]+)\\s+(\\w+)\\s*\\(([^)]*)\\)");
    private static final Pattern CLASS_IN_HANDLER = Pattern.compile("\\btype\\s*=\\s*([\\w.]+)");
    private static final Pattern PRIORITY_IN_HANDLER = Pattern.compile("\\bpriority\\s*=\\s*([\\w.]+)");
    private static final Pattern IGNORE_CANCELLED = Pattern.compile("\\bignoreCancelled\\s*=\\s*(true|false)");

    private ListenerGenerator() {
    }

    public static void main(String[] args) throws IOException {
        Path sourceRoot = null;
        String basePackage = null;
        Path outputRoot = null;
        String mainClass = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--source")) {
                sourceRoot = Path.of(args[++i]);
            } else if (arg.equals("--package")) {
                basePackage = args[++i];
            } else if (arg.equals("--output")) {
                outputRoot = Path.of(args[++i]);
            } else if (arg.equals("--main")) {
                mainClass = args[++i];
            } else {
                throw new IllegalArgumentException("Unknown argument: " + arg);
            }
        }

        if (sourceRoot == null || basePackage == null || outputRoot == null) {
            throw new IllegalArgumentException(
                    "Usage: ListenerGenerator --source <dir> --package <base.package> --output <dir> [--main <fully.qualified.MainClass>]");
        }

        ScanResult result = scan(sourceRoot);
        String generated = generate(basePackage, result.handlers(), mainClass);
        Path outputDir = outputRoot.resolve(basePackage.replace('.', '/')).resolve("generated");
        Files.createDirectories(outputDir);
        Path output = outputDir.resolve("GeneratedListeners.java");
        Files.writeString(output, generated, StandardCharsets.UTF_8);
        Files.writeString(outputDir.resolve("GeneratedCommands.java"), generateCommands(basePackage, result.commands()), StandardCharsets.UTF_8);
        if (mainClass != null) {
            Files.writeString(outputDir.resolve("GeneratedPlugin.java"), generatePlugin(basePackage, mainClass), StandardCharsets.UTF_8);
        }
        System.out.println("Generated " + output + " with " + result.handlers().size() + " handler(s)");
    }

    private static ScanResult scan(Path root) throws IOException {
        List<Handler> handlers = new ArrayList<>();
        List<String> commands = new ArrayList<>();
        try (var files = Files.walk(root)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                ScanResult fileResult = scanFile(file);
                handlers.addAll(fileResult.handlers());
                commands.addAll(fileResult.commands());
            }
        }
        return new ScanResult(handlers, commands);
    }

    private static ScanResult scanFile(Path file) throws IOException {
        String source = Files.readString(file, StandardCharsets.UTF_8);
        Matcher packageMatcher = PACKAGE_PATTERN.matcher(source);
        if (!packageMatcher.find()) {
            return new ScanResult(List.of(), List.of());
        }
        String packageName = packageMatcher.group(1);
        Matcher classMatcher = CLASS_PATTERN.matcher(source);
        if (!classMatcher.find()) {
            return new ScanResult(List.of(), List.of());
        }
        String className = classMatcher.group(1);
        String qualifiedClass = packageName + "." + className;

        List<String> commands = new ArrayList<>();
        Matcher commandMatcher = COMMAND_PATTERN.matcher(source);
        while (commandMatcher.find()) {
            commands.add(packageName + "." + commandMatcher.group(1));
        }

        List<Handler> handlers = new ArrayList<>();
        Matcher handlerMatcher = HANDLER_PATTERN.matcher(source);
        while (handlerMatcher.find()) {
            String annotation = handlerMatcher.group(1);
            String eventType = extract(annotation, CLASS_IN_HANDLER, "EventType.PLAYER_JOIN_EVENT");
            String priority = extract(annotation, PRIORITY_IN_HANDLER, "EventPriority.NORMAL");
            boolean ignoreCancelled = Boolean.parseBoolean(extract(annotation, IGNORE_CANCELLED, "false"));
            String parameter = handlerMatcher.group(4).trim();
            String eventClass = parameter.isEmpty() ? "Event" : parameter.split("\\s+")[0];
            handlers.add(new Handler(qualifiedClass, handlerMatcher.group(3), eventClass,
                    eventType, priority, ignoreCancelled));
        }
        return new ScanResult(handlers, commands);
    }

    private static String extract(String annotation, Pattern pattern, String fallback) {
        Matcher matcher = pattern.matcher(annotation);
        return matcher.find() ? matcher.group(1) : fallback;
    }

    private static String generate(String basePackage, List<Handler> handlers, String mainClass) {
        StringBuilder out = new StringBuilder();
        out.append("package ").append(basePackage).append(".generated;\n\n");
        out.append("import dev.j2kt.pumpkin.abs.PumpkinAbsPlugin;\n");
        out.append("import pumpkin.Event;\n");
        out.append("import pumpkin.EventPriority;\n");
        out.append("import pumpkin.EventType;\n\n");
        out.append("public final class GeneratedListeners {\n");

        Map<String, String> fields = new LinkedHashMap<>();
        int index = 0;
        for (Handler handler : handlers) {
            if (!fields.containsKey(handler.owner())) {
                String field = "LISTENER_" + index++;
                fields.put(handler.owner(), field);
                out.append("    private static final ").append(handler.owner()).append(' ')
                        .append(field).append(" = new ").append(handler.owner()).append("();\n");
            }
        }

        out.append("\n");
        out.append("    public static void register(PumpkinAbsPlugin plugin) {\n");
        for (Handler handler : handlers) {
            out.append("        plugin.listen(")
                    .append(handler.eventType()).append(", ")
                    .append(handler.priority()).append(", ")
                    .append(handler.ignoreCancelled()).append(",\n");
            out.append("                (server, event) -> {\n");
            out.append("                    if (event instanceof ").append(handler.eventClass()).append(" value) {\n");
            out.append("                        ").append(fields.get(handler.owner())).append('.')
                    .append(handler.method()).append("(value);\n");
            out.append("                    }\n");
            out.append("                    return event;\n");
            out.append("                });\n");
        }
        out.append("    }\n}\n");
        return out.toString();
    }

    private static String generateCommands(String basePackage, List<String> commands) {
        StringBuilder out = new StringBuilder();
        out.append("package ").append(basePackage).append(".generated;\n\n");
        out.append("import dev.j2kt.pumpkin.abs.PumpkinAbsPlugin;\n\n");
        out.append("public final class GeneratedCommands {\n");
        out.append("    public static void register(PumpkinAbsPlugin plugin) {\n");
        for (String command : commands) {
            out.append("        plugin.registerCommand(new ").append(command).append("().command());\n");
        }
        out.append("    }\n}\n");
        return out.toString();
    }

    private static String generatePlugin(String basePackage, String mainClass) {
        return "package " + basePackage + ".generated;\n\n"
                + "public final class GeneratedPlugin extends " + mainClass + " {\n"
                + "    @Override\n"
                + "    protected void onEnable() {\n"
                + "        super.onEnable();\n"
                + "        GeneratedCommands.register(this);\n"
                + "        GeneratedListeners.register(this);\n"
                + "    }\n"
                + "}\n";
    }

    private record ScanResult(List<Handler> handlers, List<String> commands) {
    }

    private record Handler(
            String owner,
            String method,
            String eventClass,
            String eventType,
            String priority,
            boolean ignoreCancelled) {
    }
}
