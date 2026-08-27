package dev.rinchan.killconfirmed.portable;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SourceContractTest {
    private static final Path ROOT = Path.of(System.getProperty("killConfirmed.repoRoot"));

    @Test
    void both_loaders_hook_real_death_and_respawn_without_calling_player_die() throws IOException {
        String fabric = read("fabric/src/main/java/dev/rinchan/killconfirmed/fabric/KillConfirmedFabric.java");
        String neoForge = read("neoforge/src/main/java/dev/rinchan/killconfirmed/neoforge/KillConfirmedNeoForge.java");
        String allJava = allSource(".java");

        assertTrue(fabric.contains("ServerLivingEntityEvents.AFTER_DEATH"));
        assertTrue(fabric.contains("ServerPlayerEvents.COPY_FROM"));
        assertTrue(fabric.contains("ServerPlayerEvents.AFTER_RESPAWN"));
        assertTrue(neoForge.contains("LivingDeathEvent"));
        assertTrue(neoForge.contains("PlayerEvent.Clone"));
        assertFalse(allJava.contains("player.die("));
        assertFalse(allJava.contains("ServerPlayer.die("));
    }

    @Test
    void loader_metadata_requires_client_and_server_rinlib_1_0_0() throws IOException {
        String fabric = read("fabric/src/main/resources/fabric.mod.json");
        String fabricMain = read("fabric/src/main/java/dev/rinchan/killconfirmed/fabric/KillConfirmedFabric.java");
        String fabricClient = read("fabric/src/main/java/dev/rinchan/killconfirmed/fabric/KillConfirmedFabricClient.java");
        String neoForge = read("neoforge/src/main/templates/META-INF/neoforge.mods.toml");
        String neoForgeMain = read("neoforge/src/main/java/dev/rinchan/killconfirmed/neoforge/KillConfirmedNeoForge.java");

        assertTrue(fabric.contains("\"environment\": \"*\""));
        assertTrue(fabric.contains("\"rinlib\": \">=${rinlib_version}\""));
        assertTrue(fabricMain.contains("ServerPlayNetworking.canSend"));
        assertTrue(fabricMain.contains("ServerPlayNetworking.send"));
        assertTrue(fabricClient.contains("ClientPlayNetworking.registerGlobalReceiver"));
        assertTrue(neoForge.contains("modId=\"rinlib\""));
        assertTrue(neoForge.contains("side=\"BOTH\""));
        assertTrue(neoForge.contains("versionRange=\"[${rinlib_version},)\""));
        assertTrue(neoForgeMain.contains("RegisterPayloadHandlersEvent"));
        assertTrue(neoForgeMain.contains("playToClient"));
        assertFalse(neoForgeMain.contains(".optional()"));
    }

    @Test
    void port_pins_26_2_and_exact_modrinth_rinlib_artifacts() throws IOException {
        String properties = read("gradle.properties");
        String rootBuild = read("build.gradle");
        String settings = read("settings.gradle");
        String fabric = read("fabric/build.gradle");
        String neoForge = read("neoforge/build.gradle");
        String readme = read("README.md");

        assertTrue(properties.contains("minecraft_version=26.2"));
        assertTrue(properties.contains("java_version=25"));
        assertTrue(properties.contains("rinlib_version=1.0.0"));
        assertTrue(rootBuild.contains("maven { url = 'https://api.modrinth.com/maven' }"));
        assertFalse(rootBuild.contains("mavenLocal()"));
        assertFalse(settings.contains("mavenLocal()"));
        assertTrue(fabric.contains(
                "maven.modrinth:rinlib:${root.property('rinlib_version')}+${root.property('minecraft_version')}-fabric"));
        assertTrue(neoForge.contains(
                "maven.modrinth:rinlib:${rootProject.rinlib_version}+${rootProject.minecraft_version}-neoforge"));
        assertTrue(readme.contains("Identifier.fromNamespaceAndPath"));
        assertFalse(readme.contains("ResourceLocation"));
    }

    @Test
    void combined_resources_cover_exact_client_and_server_pack_formats() throws IOException {
        var pack = JsonParser.parseString(read("common/src/main/resources/pack.mcmeta"))
                .getAsJsonObject().getAsJsonObject("pack");
        assertFalse(pack.has("pack_format"));
        assertEquals(88, pack.getAsJsonArray("min_format").get(0).getAsInt());
        assertEquals(0, pack.getAsJsonArray("min_format").get(1).getAsInt());
        assertEquals(107, pack.getAsJsonArray("max_format").get(0).getAsInt());
        assertEquals(1, pack.getAsJsonArray("max_format").get(1).getAsInt());
    }

    @Test
    void modern_item_definition_selects_the_existing_dog_tag_model() throws IOException {
        var item = JsonParser.parseString(read(
                "common/src/main/resources/assets/kill_confirmed/items/dog_tag.json"))
                .getAsJsonObject().getAsJsonObject("model");
        assertEquals("minecraft:model", item.get("type").getAsString());
        assertEquals("kill_confirmed:item/dog_tag", item.get("model").getAsString());
        assertTrue(Files.isRegularFile(ROOT.resolve(
                "common/src/main/resources/assets/kill_confirmed/models/item/dog_tag.json")));
        assertTrue(Files.isRegularFile(ROOT.resolve(
                "common/src/main/resources/assets/kill_confirmed/textures/item/dog_tag.png")));
    }

    @Test
    void both_builds_compile_the_same_common_source() throws IOException {
        String fabric = read("fabric/build.gradle");
        String neoForge = read("neoforge/build.gradle");
        assertTrue(fabric.contains("common/src/main/java"));
        assertTrue(neoForge.contains("common/src/main/java"));
    }

    @Test
    void datapack_bridge_owns_versioned_scratch_storage_and_cleanup() throws IOException {
        String bridge = read("common/src/main/java/dev/rinchan/killconfirmed/DatapackPlaceholderBridge.java");
        assertTrue(Files.isRegularFile(ROOT.resolve(
                "common/src/main/resources/data/kill_confirmed/tags/function/placeholder_providers.json")));
        assertTrue(bridge.contains("KillConfirmed.id(\"scratch\")"));
        assertTrue(bridge.contains("input.putInt(\"schema\", 1)"));
        assertTrue(bridge.contains("finally"));
        assertTrue(bridge.contains("storage.set(SCRATCH_STORAGE, new CompoundTag())"));
    }

    @Test
    void private_scope_is_absent_from_source_and_resources() throws IOException {
        String content = allSource(".java", ".json", ".toml", ".md").toLowerCase();
        for (String forbidden : new String[]{
                "dog_tag_case", "puffish", "wandering_trader", "wmf_", "watermelon field",
                "recipe serializer", "skills", "progression", "screenhandler", "menu_type"}) {
            assertFalse(content.contains(forbidden), () -> "Forbidden private-scope marker found: " + forbidden);
        }
    }

    private static String read(String path) throws IOException { return Files.readString(ROOT.resolve(path)); }

    private static String allSource(String... suffixes) throws IOException {
        try (var paths = Files.walk(ROOT)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> java.util.Arrays.stream(suffixes).anyMatch(suffix -> path.toString().endsWith(suffix)))
                    .filter(path -> !path.toString().contains("/build/"))
                    .filter(path -> !path.toString().contains("/.gradle/"))
                    .filter(path -> !path.toString().contains("/src/test/"))
                    .map(path -> {
                        try { return Files.readString(path); }
                        catch (IOException exception) { throw new java.io.UncheckedIOException(exception); }
                    })
                    .collect(java.util.stream.Collectors.joining("\n"));
        }
    }
}
