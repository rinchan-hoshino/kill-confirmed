package dev.rinchan.killconfirmed.rinlib.v2;

import java.nio.file.Path;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
public interface Rin {
    Path configDir();
    Supplier<Item> registerItem(String path, Supplier<Item> factory);
    void requireOnClient(String networkVersion);
    String modId();
}
