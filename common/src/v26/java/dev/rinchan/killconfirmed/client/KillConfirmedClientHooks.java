package dev.rinchan.killconfirmed.client;

import dev.rinchan.killconfirmed.KillConfirmed;
import net.minecraft.client.Minecraft;

/** Client-only hooks registered exclusively on the client distribution. */
public final class KillConfirmedClientHooks {
    private KillConfirmedClientHooks() {}

    public static void init() {
        KillConfirmed.bindShiftDown(() -> Minecraft.getInstance().hasShiftDown());
    }
}
