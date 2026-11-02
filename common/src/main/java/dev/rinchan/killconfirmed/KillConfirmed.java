package dev.rinchan.killconfirmed;

import java.nio.file.Path;
import dev.rinchan.rinlib.v2.PlayerData;
import dev.rinchan.rinlib.v2.PlayerEvents;
import dev.rinchan.rinlib.v2.Rin;
import dev.rinchan.rinlib.v2.RinMod;
import dev.rinchan.rinlib.api.ItemProperties;
import net.minecraft.world.item.Rarity;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class KillConfirmed implements RinMod {
    public static final String MOD_ID = "kill_confirmed";
    public static final String NETWORK_VERSION = "1";
    public static final Logger LOGGER = LoggerFactory.getLogger("Kill Confirmed");
    public static final ResourceLocation DOG_TAG_ID = id("dog_tag");

    // Register the save key before players load, including when lifecycle callbacks have not run yet.
    static final PlayerData.Slot PENDING_DOG_TAG = PlayerData.slot("KillConfirmedPendingDogTag");
    private static Supplier<Item> dogTag;
    private static KillConfirmedConfig config;

    public KillConfirmed() {}

    @Override
    public void init(Rin env) {
        var item = env.registerItem("dog_tag", KillConfirmed::createDogTag);
        initialize(item, env.configDir());
        env.requireOnClient(NETWORK_VERSION);
        PlayerEvents.DEATH.register(DogTagLifecycle::onDeath);
        PlayerEvents.COPY_AFTER_DEATH.register(DogTagLifecycle::copyPending);
        PlayerEvents.RESPAWN.register(DogTagLifecycle::onRespawn);
    }

    public static Item createDogTag() {
        return new Item(ItemProperties.of(DOG_TAG_ID.toString()).stacksTo(1).rarity(Rarity.RARE));
    }

    public static synchronized void initialize(Supplier<Item> dogTagSupplier, Path configDirectory) {
        if (dogTag != null) throw new IllegalStateException("Kill Confirmed is already initialized");
        dogTag = Objects.requireNonNull(dogTagSupplier, "dogTagSupplier");
        config = KillConfirmedConfig.load(Objects.requireNonNull(configDirectory, "configDirectory"));
        if (java.nio.file.Files.exists(configDirectory.resolve("kill_confirmed.json")))
            LOGGER.warn("old config ignored, see kill_confirmed.toml");
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
