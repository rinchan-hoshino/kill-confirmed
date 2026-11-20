package dev.rinchan.killconfirmed.portable;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Automated architectural guardrails scanning real built packaged JARs:
 * 1. Dedicated Server isolation (zero net/minecraft/client/ or client GUI references outside client packages).
 * 2. Mixin bidirectional closure (*.mixins.json <-> packaged .class <-> @Mixin source classes).
 * 3. Clean artifact hygiene (zero rinlib, zero com/terraformersmc/modmenu compile-time stub leaks).
 */
class ArchitectureContractTest {
    private static final List<String> FORBIDDEN_CLIENT_REFS = List.of(
            "net/minecraft/client/",
            "net/neoforged/neoforge/client/",
            "net/minecraftforge/client/",
            "dev/rinchan/killconfirmed/client/");

    private static final Pattern MIXIN_PACKAGE = Pattern.compile("\"package\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern MIXIN_ARRAY = Pattern.compile("\"(?:mixins|client|server)\"\\s*:\\s*\\[([^\\]]*)\\]");
    private static final Pattern JSON_STRING = Pattern.compile("\"([^\"]+)\"");
    private static final Pattern JAVA_PACKAGE = Pattern.compile("(?m)^\\s*package\\s+([a-zA-Z0-9_.]+)\\s*;");
    private static final Pattern MIXIN_ANNOTATION = Pattern.compile("(?m)^\\s*@Mixin\\s*\\(");

    @Test
    void dedicated_server_isolation_zero_client_refs_outside_client_packages() throws IOException {
        List<Path> jars = targetJars();
        assertFalse(jars.isEmpty(), "Expected at least one built JAR to audit");

        List<String> violations = new ArrayList<>();
        for (Path jarPath : jars) {
            try (JarFile jar = new JarFile(jarPath.toFile())) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (!name.endsWith(".class") || isClientPackage(name)) continue;

                    byte[] bytes = jar.getInputStream(entry).readAllBytes();
                    for (String utf8 : extractConstantPoolUtf8(bytes)) {
                        for (String forbidden : FORBIDDEN_CLIENT_REFS) {
                            if (utf8.contains(forbidden)) {
                                violations.add(jarPath.getFileName() + " :: " + name + " -> " + utf8);
                            }
                        }
                    }
                }
            }
        }
        assertTrue(violations.isEmpty(),
                "Dedicated Server isolation violated (client references outside client packages):\n"
                        + String.join("\n", violations));
    }

    @Test
    void mixin_bidirectional_closure_between_config_jar_and_source() throws IOException {
        Path primaryJar = primaryJar();
        Path root = projectRoot();
        String apiLayer = System.getProperty("killconfirmed.apiLayer", "v121");
        String loader = System.getProperty("killconfirmed.loader", "neoforge");

        Set<String> declaredMixins = new HashSet<>();
        Set<String> jarClasses = new HashSet<>();
        int mixinConfigCount = 0;

        try (JarFile jar = new JarFile(primaryJar.toFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.endsWith(".class")) {
                    jarClasses.add(name);
                } else if (name.endsWith(".mixins.json")) {
                    mixinConfigCount++;
                    String json = new String(jar.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8);
                    declaredMixins.addAll(parseMixinClasses(json));
                }
            }
        }

        assertTrue(mixinConfigCount > 0, "No *.mixins.json found in " + primaryJar);
        assertFalse(declaredMixins.isEmpty(), "No Mixin classes declared in *.mixins.json of " + primaryJar);

        // Direction 1: every mixin declared in *.mixins.json must exist as a .class in the packaged JAR
        List<String> missingInJar = new ArrayList<>();
        for (String fqcn : declaredMixins) {
            String internalPath = fqcn.replace('.', '/') + ".class";
            if (!jarClasses.contains(internalPath)) {
                missingInJar.add(fqcn + " (missing " + internalPath + ")");
            }
        }
        assertTrue(missingInJar.isEmpty(),
                "Declared mixins missing from packaged JAR " + primaryJar.getFileName() + ":\n"
                        + String.join("\n", missingInJar));

        // Direction 2: every @Mixin class in source for this target must be declared in *.mixins.json
        Set<String> sourceMixins = new HashSet<>();
        for (Path srcDir : List.of(
                root.resolve("common/src/main/java"),
                root.resolve("common/src/" + apiLayer + "/java"),
                root.resolve(loader + "/src/main/java"))) {
            if (!Files.isDirectory(srcDir)) continue;
            try (var stream = Files.walk(srcDir)) {
                for (Path file : stream.filter(p -> p.toString().endsWith(".java")).toList()) {
                    String content = Files.readString(file, StandardCharsets.UTF_8);
                    if (MIXIN_ANNOTATION.matcher(content).find()) {
                        Matcher pkg = JAVA_PACKAGE.matcher(content);
                        assertTrue(pkg.find(), "Missing package declaration in " + file);
                        String simpleName = file.getFileName().toString().replace(".java", "");
                        sourceMixins.add(pkg.group(1) + "." + simpleName);
                    }
                }
            }
        }

        assertFalse(sourceMixins.isEmpty(), "Expected @Mixin classes in source tree");
        List<String> unregistered = new ArrayList<>();
        for (String srcMixin : sourceMixins) {
            if (!declaredMixins.contains(srcMixin)) {
                unregistered.add(srcMixin);
            }
        }
        assertTrue(unregistered.isEmpty(),
                "@Mixin source classes not registered in any *.mixins.json:\n" + String.join("\n", unregistered));

        // Also validate Direction 1 across all release JARs when scanning full matrix
        for (Path jarPath : targetJars()) {
            if (jarPath.equals(primaryJar)) continue;
            Set<String> classes = new HashSet<>();
            Set<String> mixins = new HashSet<>();
            try (JarFile jar = new JarFile(jarPath.toFile())) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().endsWith(".class")) {
                        classes.add(entry.getName());
                    } else if (entry.getName().endsWith(".mixins.json")) {
                        String json = new String(jar.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8);
                        mixins.addAll(parseMixinClasses(json));
                    }
                }
            }
            assertFalse(mixins.isEmpty(), "No mixins declared in " + jarPath.getFileName());
            for (String fqcn : mixins) {
                assertTrue(classes.contains(fqcn.replace('.', '/') + ".class"),
                        "Missing mixin class " + fqcn + " in " + jarPath.getFileName());
            }
        }
    }

    @Test
    void packaged_jars_contain_zero_rinlib_and_zero_modmenu_stub_leaks() throws IOException {
        List<Path> jars = targetJars();
        assertFalse(jars.isEmpty(), "Expected at least one built JAR to audit");

        List<String> leaks = new ArrayList<>();
        for (Path jarPath : jars) {
            try (JarFile jar = new JarFile(jarPath.toFile())) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    String lower = name.toLowerCase(Locale.ROOT);
                    if (lower.contains("rinlib")) {
                        leaks.add(jarPath.getFileName() + " :: forbidden rinlib entry: " + name);
                    }
                    if (name.startsWith("com/terraformersmc/modmenu/")) {
                        leaks.add(jarPath.getFileName() + " :: leaked modmenu stub entry: " + name);
                    }
                    if (name.endsWith(".class")) {
                        byte[] bytes = jar.getInputStream(entry).readAllBytes();
                        for (String utf8 : extractConstantPoolUtf8(bytes)) {
                            if (utf8.toLowerCase(Locale.ROOT).contains("rinlib")) {
                                leaks.add(jarPath.getFileName() + " :: rinlib ref in " + name + " -> " + utf8);
                            }
                        }
                    }
                }
            }
        }
        assertTrue(leaks.isEmpty(),
                "Clean package red line violated:\n" + String.join("\n", leaks));
    }

    private static boolean isClientPackage(String entryName) {
        return entryName.contains("/client/");
    }

    private static Set<String> parseMixinClasses(String json) {
        Set<String> result = new HashSet<>();
        Matcher pkgMatcher = MIXIN_PACKAGE.matcher(json);
        if (!pkgMatcher.find()) return result;
        String basePkg = pkgMatcher.group(1);

        Matcher arrMatcher = MIXIN_ARRAY.matcher(json);
        while (arrMatcher.find()) {
            Matcher strMatcher = JSON_STRING.matcher(arrMatcher.group(1));
            while (strMatcher.find()) {
                result.add(basePkg + "." + strMatcher.group(1));
            }
        }
        return result;
    }

    private static Path primaryJar() {
        String configured = System.getProperty("killconfirmed.jar");
        if (configured != null && !configured.isBlank()) {
            Path p = Path.of(configured);
            assertTrue(Files.isRegularFile(p), "Configured JAR does not exist: " + p);
            return p;
        }
        Path root = projectRoot();
        String modVersion = readModVersion(root);
        Path defaultJar = root.resolve("build/profiles/1.21.1/neoforge/libs/kill_confirmed-neoforge-" + modVersion + "+1.21.1.jar");
        assertTrue(Files.isRegularFile(defaultJar),
                "Primary built JAR not found (run :neoforge:jar first): " + defaultJar);
        return defaultJar;
    }

    private static String readModVersion(Path root) {
        Path props = root.resolve("gradle.properties");
        try {
            for (String line : Files.readAllLines(props)) {
                String trimmed = line.trim();
                if (trimmed.startsWith("mod_version=")) {
                    return trimmed.substring("mod_version=".length()).trim();
                }
            }
        } catch (IOException ignored) {
        }
        throw new IllegalStateException("mod_version not found in " + props);
    }

    private static List<Path> targetJars() throws IOException {
        List<Path> jars = new ArrayList<>();
        jars.add(primaryJar());
        if (Boolean.getBoolean("killconfirmed.scanAllReleases")) {
            Path releases = projectRoot().resolve("build/releases");
            if (Files.isDirectory(releases)) {
                try (var stream = Files.list(releases)) {
                    stream.filter(p -> p.getFileName().toString().endsWith(".jar"))
                            .sorted()
                            .forEach(jars::add);
                }
            }
        }
        return jars;
    }

    private static Path projectRoot() {
        String prop = System.getProperty("killconfirmed.projectRoot");
        if (prop != null && !prop.isBlank()) {
            return Path.of(prop);
        }
        Path cwd = Path.of("").toAbsolutePath();
        return Files.exists(cwd.resolve("profiles.json")) ? cwd : cwd.getParent();
    }

    /** Minimal JVMS §4.4 constant-pool parser extracting all CONSTANT_Utf8 strings. */
    private static List<String> extractConstantPoolUtf8(byte[] classBytes) throws IOException {
        List<String> utf8s = new ArrayList<>();
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(classBytes))) {
            int magic = in.readInt();
            assertEquals(0xCAFEBABE, magic, "Invalid .class magic header");
            in.readUnsignedShort(); // minor
            in.readUnsignedShort(); // major
            int cpCount = in.readUnsignedShort();
            for (int i = 1; i < cpCount; i++) {
                int tag = in.readUnsignedByte();
                switch (tag) {
                    case 1 -> utf8s.add(in.readUTF()); // CONSTANT_Utf8
                    case 3, 4, 9, 10, 11, 12, 17, 18 -> in.skipBytes(4);
                    case 5, 6 -> { // CONSTANT_Long / CONSTANT_Double occupy 2 slots
                        in.skipBytes(8);
                        i++;
                    }
                    case 7, 8, 16, 19, 20 -> in.skipBytes(2);
                    case 15 -> in.skipBytes(3); // CONSTANT_MethodHandle
                    default -> throw new IOException("Unsupported constant pool tag " + tag + " at index " + i);
                }
            }
        }
        return utf8s;
    }
}
