package dev.rinchan.killconfirmed;

import dev.rinchan.killconfirmed.portable.DeathCooldownTracker;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Shared mod state, registry accessors, and configuration lifecycle for Kill Confirmed 2.0. */
public final class KillConfirmed {
    public static final String MOD_ID = "kill_confirmed";
    public static final Logger LOGGER = LoggerFactory.getLogger("Kill Confirmed");

    private static final DeathCooldownTracker COOLDOWN_TRACKER = new DeathCooldownTracker();

    private static volatile KillConfirmedConfig config = KillConfirmedConfig.defaults();
    private static volatile Path configDir;
    private static Supplier<Item> dogTagSupplier;
    private static Supplier<Item> dogTagCaseSupplier;
    private static Supplier<MenuType<?>> dogTagCaseMenuSupplier;
    private static volatile BooleanSupplier shiftDownSupplier = () -> false;

    private KillConfirmed() {}

    public static synchronized void initConfig(Path directory) {
        configDir = Objects.requireNonNull(directory, "configDirectory");
        config = KillConfirmedConfig.load(configDir);
        if (Files.exists(configDir.resolve("kill_confirmed.json"))) {
            LOGGER.warn("Legacy kill_confirmed.json ignored; using kill_confirmed.toml");
        }
        LOGGER.info("Kill Confirmed 2.0 initialized (drop={}, fireResistant={}, cooldown={}s)",
                config.dropStrategy(), config.fireResistant(), config.cooldownSeconds());
    }

    public static synchronized void bindRegistries(
            Supplier<Item> dogTag,
            Supplier<Item> dogTagCase,
            Supplier<MenuType<?>> dogTagCaseMenu) {
        dogTagSupplier = Objects.requireNonNull(dogTag, "dogTag");
        dogTagCaseSupplier = Objects.requireNonNull(dogTagCase, "dogTagCase");
        dogTagCaseMenuSupplier = Objects.requireNonNull(dogTagCaseMenu, "dogTagCaseMenu");
    }

    public static Item dogTag() {
        if (dogTagSupplier == null) throw new IllegalStateException("Kill Confirmed items are not bound");
        return dogTagSupplier.get();
    }

    public static Item dogTagCase() {
        if (dogTagCaseSupplier == null) throw new IllegalStateException("Kill Confirmed items are not bound");
        return dogTagCaseSupplier.get();
    }

    public static MenuType<?> dogTagCaseMenuType() {
        if (dogTagCaseMenuSupplier == null) throw new IllegalStateException("Kill Confirmed menu is not bound");
        return dogTagCaseMenuSupplier.get();
    }

    public static KillConfirmedConfig config() {
        return config;
    }

    public static synchronized void saveConfig(KillConfirmedConfig updated) {
        Objects.requireNonNull(updated, "updated");
        config = updated;
        if (configDir != null) {
            updated.save(configDir);
        }
    }

    public static DeathCooldownTracker cooldownTracker() {
        return COOLDOWN_TRACKER;
    }

    public static void bindShiftDown(BooleanSupplier supplier) {
        shiftDownSupplier = Objects.requireNonNull(supplier, "shiftDownSupplier");
    }

    public static boolean isShiftDown() {
        return shiftDownSupplier.getAsBoolean();
    }
}
