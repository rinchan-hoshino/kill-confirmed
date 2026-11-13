package dev.rinchan.killconfirmed.neoforge;

import dev.rinchan.killconfirmed.KillConfirmed;
import dev.rinchan.killconfirmed.rinlib.v2.RinNeoForge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(KillConfirmed.MOD_ID)
public final class KillConfirmedNeoForge {
    public KillConfirmedNeoForge(IEventBus modBus) {
        new RinNeoForge("kill_confirmed", modBus).start(new KillConfirmed());
    }
}
