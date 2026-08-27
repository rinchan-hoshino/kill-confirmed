package dev.rinchan.killconfirmed;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class KillConfirmed {
    public static final String MOD_ID = "kill_confirmed";
    public static final String NETWORK_VERSION = "1";
    public static final Logger LOGGER = LoggerFactory.getLogger("Kill Confirmed");
    public static final ResourceLocation DOG_TAG_ID = id("dog_tag");

    private static Supplier<Item> dogTag;
    private static KillConfirmedConfig config;

    private KillConfirmed() {}

    public static synchronized void initialize(Supplier<Item> dogTagSupplier, Path configDirectory) {
        if (dogTag != null) throw new IllegalStateException("Kill Confirmed is already initialized");
        dogTag = Objects.requireNonNull(dogTagSupplier, "dogTagSupplier");
        config = KillConfirmedConfig.load(Objects.requireNonNull(configDirectory, "configDirectory"));
        LOGGER.info("Kill Confirmed initialized with drop strategy {}", config.dropStrategy());
    }

    public static Item dogTag() {
        if (dogTag == null) throw new IllegalStateException("Kill Confirmed is not initialized");
        return dogTag.get();
    }

    public static KillConfirmedConfig config() {
        if (config == null) throw new IllegalStateException("Kill Confirmed is not initialized");
        return config;
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
