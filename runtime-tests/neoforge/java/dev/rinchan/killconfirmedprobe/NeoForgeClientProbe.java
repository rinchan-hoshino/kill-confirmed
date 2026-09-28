package dev.rinchan.killconfirmedprobe;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
@EventBusSubscriber(modid="kill_confirmed_probe", value=Dist.CLIENT)
public final class NeoForgeClientProbe {
    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) { ClientProbe.tick(); }
}
