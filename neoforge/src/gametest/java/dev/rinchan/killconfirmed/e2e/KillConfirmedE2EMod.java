package dev.rinchan.killconfirmed.e2e;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@Mod("kill_confirmed_e2e")
public final class KillConfirmedE2EMod {
    public KillConfirmedE2EMod(IEventBus modBus) {
        modBus.addListener(this::registerGameTests);
    }

    private void registerGameTests(RegisterGameTestsEvent event) {
        event.register(KillConfirmedPackagedJarGameTests.class);
    }
}
