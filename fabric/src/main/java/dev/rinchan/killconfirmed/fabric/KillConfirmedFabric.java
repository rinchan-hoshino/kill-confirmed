package dev.rinchan.killconfirmed.fabric;

import dev.rinchan.killconfirmed.KillConfirmed;
import dev.rinchan.rinlib.v2.RinFabric;
import net.fabricmc.api.ModInitializer;

public final class KillConfirmedFabric implements ModInitializer {
    @Override public void onInitialize() {
        new RinFabric("kill_confirmed").start(new KillConfirmed());
    }
}
