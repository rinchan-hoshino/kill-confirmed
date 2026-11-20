package dev.rinchan.killconfirmed.fabric;

import dev.rinchan.killconfirmed.KillConfirmed;
import dev.rinchan.killconfirmed.VersionFactory;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class KillConfirmedFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        KillConfirmed.initConfig(FabricLoader.getInstance().getConfigDir());
        VersionFactory.registerFabric();
    }
}
